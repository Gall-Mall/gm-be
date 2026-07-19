package com.gm.mq.voteresult;

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
 * 투표 결과 반환(4번) 도메인 큐 토폴로지.
 *
 * 1개의 vote-result q와 1개의 dlq 생성.
 * app.events(topic)에 2개 키 바인딩: group.survey.list.ready, group.store.search.completed
 * (B형 완료 이벤트를 받아 WebSocket으로 화면에 push)
 */
@Configuration
public class VoteResultQueueConfig {

    public static final String VOTE_RESULT_QUEUE = "vote-result.events.q";
    public static final String VOTE_RESULT_DLQ = "vote-result.events.dlq";
    public static final String VOTE_RESULT_DLQ_KEY = "vote-result.dlq";

    @Bean
    public Queue voteResultQueue() {
        return QueueBuilder.durable(VOTE_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.APP_DLX)
                .withArgument("x-dead-letter-routing-key", VOTE_RESULT_DLQ_KEY)
                .build();
    }

    @Bean
    public Queue voteResultDlq() {
        return QueueBuilder.durable(VOTE_RESULT_DLQ).build();
    }

    @Bean
    public Declarables voteResultBindings(Queue voteResultQueue, TopicExchange appExchange) {
        return new Declarables(
                BindingBuilder.bind(voteResultQueue).to(appExchange).with("group.survey.list.ready"),
                BindingBuilder.bind(voteResultQueue).to(appExchange).with("group.store.search.completed")
        );
    }

    @Bean
    public Binding voteResultDlqBinding(Queue voteResultDlq, DirectExchange appExchangeDlx) {
        return BindingBuilder.bind(voteResultDlq).to(appExchangeDlx).with(VOTE_RESULT_DLQ_KEY);
    }
}
