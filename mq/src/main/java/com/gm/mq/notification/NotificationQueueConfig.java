package com.gm.mq.notification;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gm.mq.config.RabbitMQConfig;

/**
 * Notifiaction Queue 설정
 *
 * 1개의 noti q와 1개의 noti dlq 생성
 * 도메인 별로 각자의 dlq를 생성하여 topicExchange와 1 : N 을 이룸
 */
@Configuration
public class NotificationQueueConfig {

    public static final String NOTIFICATION_QUEUE = "notification.events.q";
    public static final String NOTIFICATION_DLQ = "notification.events.dlq";
    public static final String NOTIFICATION_DLQ_KEY = "notification.dlq";

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.APP_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ_KEY)
                .build();
    }

    @Bean
    public Queue notificationDlq(){
        return QueueBuilder.durable(NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange appExchange){
        return BindingBuilder
                .bind(notificationQueue)
                .to(appExchange)
                .with("vote.all.completed");
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, DirectExchange appExchangeDlx){
        return BindingBuilder
                .bind(notificationDlq)
                .to(appExchangeDlx)
                .with(NOTIFICATION_DLQ_KEY);
    }

}
