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
                RabbitMQConfig.APP_EXCHANGE,   // 항상 app.events (큐가 아니라 exchange)
                routingKey,                     // 라우팅 키 → 브로커가 바인딩 매칭
                payload,                        // 객체 → JSON 자동 변환
                message -> {                    // eventId를 messageId 프로퍼티에 (envelope 없이)
                    message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
                    return message;
                }
        );
    }
}
