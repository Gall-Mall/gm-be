package com.gm.mq.storesearch;

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
 * 식당 검색(5번) 도메인 큐 토폴로지.
 *
 * 1개의 store-search q와 1개의 dlq 생성.
 * app.events(topic)에 store.search.requested 바인딩.
 * 알림과 로직·부하가 달라 별도 큐로 분리(head-of-line blocking 방지).
 */
@Configuration
public class StoreSearchQueueConfig {

    public static final String STORE_SEARCH_QUEUE = "store-search.events.q";
    public static final String STORE_SEARCH_DLQ = "store-search.events.dlq";
    public static final String STORE_SEARCH_DLQ_KEY = "store-search.dlq";

    @Bean
    public Queue storeSearchQueue() {
        return QueueBuilder.durable(STORE_SEARCH_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.APP_DLX)
                .withArgument("x-dead-letter-routing-key", STORE_SEARCH_DLQ_KEY)
                .build();
    }

    @Bean
    public Queue storeSearchDlq() {
        return QueueBuilder.durable(STORE_SEARCH_DLQ).build();
    }

    @Bean
    public Binding storeSearchBinding(Queue storeSearchQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(storeSearchQueue).to(appExchange).with("store.search.requested");
    }

    @Bean
    public Binding storeSearchDlqBinding(Queue storeSearchDlq, DirectExchange appExchangeDlx) {
        return BindingBuilder.bind(storeSearchDlq).to(appExchangeDlx).with(STORE_SEARCH_DLQ_KEY);
    }
}
