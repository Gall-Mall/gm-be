package com.gm.mq.recommendation;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gm.core.event.payload.SurveyRequested;
import com.gm.mq.config.RabbitMQConfig;

/**
 * 추천(3번) 도메인 큐 토폴로지.
 *
 * 1개의 recommendation q와 1개의 recommendation dlq 생성.
 * app.events(topic)에 group.survey.requested 바인딩.
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
    public Binding recommendationBinding(Queue recommendationQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(recommendationQueue).to(appExchange).with(SurveyRequested.TYPE);
    }

    @Bean
    public Binding recommendationDlqBinding(Queue recommendationDlq, DirectExchange appExchangeDlx) {
        return BindingBuilder.bind(recommendationDlq).to(appExchangeDlx).with(RECOMMENDATION_DLQ_KEY);
    }
}
