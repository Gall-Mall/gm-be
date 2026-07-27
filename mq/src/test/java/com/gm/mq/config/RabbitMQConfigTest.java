package com.gm.mq.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import com.gm.mq.event.EventEnvelope;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    void messageConverter_deserializesInternalEventEnvelope() {
        JacksonJsonMessageConverter converter = new RabbitMQConfig().messageConverter();
        EventEnvelope<Map<String, String>> envelope =
                EventEnvelope.of("test.event", Map.of("id", "event-1"));
        Message message = converter.toMessage(envelope, new MessageProperties());

        Object converted = converter.fromMessage(message);

        assertThat(converted).isInstanceOf(EventEnvelope.class);
    }
}
