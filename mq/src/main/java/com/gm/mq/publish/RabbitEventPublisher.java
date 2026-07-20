package com.gm.mq.publish;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.gm.core.event.EventPublisher;
import com.gm.mq.config.RabbitMQConfig;

/**
 * 이벤트 발행 아웃바운드 어댑터.
 *
 * <p>core의 {@link EventPublisher} 포트를 구현하여 도메인 서비스가 발행한 이벤트를
 * {@code app.events}(topic exchange)로 내보낸다. payload는 JSON 변환, eventId는 messageId에 싣는다.</p>
 */
@Component
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APP_EXCHANGE,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
                    return message;
                }
        );
    }
}
