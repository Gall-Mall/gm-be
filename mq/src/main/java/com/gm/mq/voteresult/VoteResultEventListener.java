package com.gm.mq.voteresult;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.gm.core.event.payload.StoreSearchCompleted;
import com.gm.core.event.payload.SurveyListReady;
import com.gm.mq.support.EventConsumer;

/**
 * 투표 결과 반환(4번) 도메인 이벤트 소비자.
 *
 * vote-result.events.q 소비 — B형 완료 이벤트 2종을 라우팅 키로 분기.
 * 완료 이벤트를 받아 core RealtimePushPort를 통해 기존 WebSocket으로 화면에 push.
 * push만 하므로 Redis 경로를 쓴다.
 */
@Slf4j
@Component
public class VoteResultEventListener {

    private final EventConsumer consumer;

    public VoteResultEventListener(@Qualifier("dedupOnlyEventConsumer") EventConsumer consumer) {
        this.consumer = consumer;
    }

    @RabbitListener(queues = VoteResultQueueConfig.VOTE_RESULT_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        switch (routingKey) {
            case SurveyListReady.TYPE -> consumer.consumeOnce(message, SurveyListReady.class, payload -> {
                log.info("[vote-result] {} 수신: {}", routingKey, payload.voteSessionId());
                // TODO: RealtimePushPort로 멤버 화면에 메뉴 후보 push
                //       (/topic/groups/{groupId}/sessions/{voteSessionId}/menus)
            });
            case StoreSearchCompleted.TYPE -> consumer.consumeOnce(message, StoreSearchCompleted.class, payload -> {
                log.info("[vote-result] {} 수신: {}", routingKey, payload.voteSessionId());
                // TODO: RealtimePushPort로 방장 화면에 식당 목록 push
                //       (/user/queue/store-search/{voteSessionId})
            });
            default -> log.warn("[vote-result] 알 수 없는 라우팅 키: {}", routingKey);
        }
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
