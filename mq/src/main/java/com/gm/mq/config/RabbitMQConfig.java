package com.gm.mq.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 공용 인프라 설정.
 */
@Configuration
public class RabbitMQConfig {

    public static final String APP_EXCHANGE = "app.events";
    public static final String APP_DLX = "app.events.dlx";

    @Bean
    public TopicExchange appExchange(){
        return new TopicExchange(APP_EXCHANGE);
    }

    @Bean
    public DirectExchange appExchangeDlx(){
        return new DirectExchange(APP_DLX);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
