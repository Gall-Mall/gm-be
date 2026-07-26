package com.gm.mq.publish;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.gm.core.event.DomainEvent;
import com.gm.core.event.EventPublisher;
import com.gm.mq.config.RabbitMQConfig;
import com.gm.mq.event.EventEnvelope;

/**
 * 이벤트 발행 아웃바운드 어댑터.
 *
 * <p>core의 {@link EventPublisher} 포트를 구현하여 도메인 이벤트를 {@link EventEnvelope}로 감싸
 * {@code app.events}(topic exchange)로 내보낸다. eventType을 라우팅 키로 쓸지는 여기서 정한다.
 * eventId는 messageId에도 실어 DLQ에서 본문과 브로커 메타데이터가 같은 값을 갖게 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = event.eventType();
        EventEnvelope<DomainEvent> envelope = EventEnvelope.of(routingKey, event);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APP_EXCHANGE,
                routingKey,
                envelope,
                message -> {
                    message.getMessageProperties().setMessageId(envelope.eventId());
                    return message;
                }
        );
    }
}
