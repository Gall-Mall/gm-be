package com.gm.mq.support;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

/**
 * 모든 {@code @RabbitListener}의 공통 에러 핸들러.
 */
@Slf4j
@Component
public class MqListenerErrorHandler implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(Message amqpMessage, Channel channel,
                              org.springframework.messaging.Message<?> message,
                              ListenerExecutionFailedException exception) throws Exception{
        var props = amqpMessage.getMessageProperties();
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

        log.error("리스너 실패: key = {}, msgId = {}",
                props.getReceivedRoutingKey(), props.getMessageId(), cause);

        // 재시도해도 성공할 수 없는 예외는 재시도를 건너뛰고 즉시 DLQ로 보낸다.
        // DataIntegrityViolationException은 넣지 않는다. 멱등 충돌은 INSERT IGNORE가
        // 예외 없이 걸러내므로, 여기 걸리는 건 비즈니스 제약 위반뿐이다.
        // 그중 이벤트 순서 역전으로 인한 FK 위반은 재시도로 풀린다.
        if(cause instanceof IllegalArgumentException){
            throw new AmqpRejectAndDontRequeueException(cause);
        }

        // 예외를 다시 던져야 재시도 인터셉터가 개입해 재시도하고, 소진되면 DLQ로 간다.
        // (여기서 return하면 정상 처리로 간주되어 ACK → 재시도도 DLQ도 없이 메시지가 사라진다)
        throw exception;
    }
}
