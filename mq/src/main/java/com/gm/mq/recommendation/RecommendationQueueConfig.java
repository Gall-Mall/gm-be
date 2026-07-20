package com.gm.mq.recommendation;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gm.mq.config.RabbitMQConfig;

/**
 * 추천(3번) 도메인 큐 토폴로지.
 *
 * 1개의 recommendation q와 1개의 recommendation dlq 생성.
 * app.events(topic)에 2개 키 바인딩: user.onboarding.submitted, group.survey.requested
 */
@Configuration
public class RecommendationQueueConfig {

    public static final String RECOMMENDATION_QUEUE = "recommendation.events.q";
    public static final String RECOMMENDATION_DLQ = "recommendation.events.dlq";
    public static final String RECOMMENDATION_DLQ_KEY = "recommendation.dlq";

    @Bean
    public Queue recommendationQueue() {
        return QueueBuilder.durable(RECOMMENDATION_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.APP_DLX)
                .withArgument("x-dead-letter-routing-key", RECOMMENDATION_DLQ_KEY)
                .build();
    }

    @Bean
    public Queue recommendationDlq() {
        return QueueBuilder.durable(RECOMMENDATION_DLQ).build();
    }

    @Bean
    public Declarables recommendationBindings(Queue recommendationQueue, TopicExchange appExchange) {
        return new Declarables(
                BindingBuilder.bind(recommendationQueue).to(appExchange).with("user.onboarding.submitted"),
                BindingBuilder.bind(recommendationQueue).to(appExchange).with("group.survey.requested")
        );
    }

    @Bean
    public Binding recommendationDlqBinding(Queue recommendationDlq, DirectExchange appExchangeDlx) {
        return BindingBuilder.bind(recommendationDlq).to(appExchangeDlx).with(RECOMMENDATION_DLQ_KEY);
    }
}
