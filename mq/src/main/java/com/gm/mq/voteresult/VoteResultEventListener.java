package com.gm.mq.voteresult;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 투표 결과 반환(4번) 도메인 이벤트 소비자.
 *
 * vote-result.events.q 소비 — B형 완료 이벤트 2종을 라우팅 키로 분기.
 * 완료 이벤트를 받아 core RealtimePushPort를 통해 기존 WebSocket으로 화면에 push.
 */
@Slf4j
@Component
public class VoteResultEventListener {

    @RabbitListener(queues = VoteResultQueueConfig.VOTE_RESULT_QUEUE)
    public void handle(Message message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("[vote-result] {} 수신: {}", routingKey, new String(message.getBody()));
        // TODO: 멱등(eventId) 체크
        switch (routingKey) {
            case "group.survey.list.ready" -> {
                // TODO: EventEnvelope<SurveyListReady> 역직렬화
                // TODO: RealtimePushPort로 멤버 화면에 메뉴 후보 push
                //       (/topic/groups/{groupId}/sessions/{voteSessionId}/menus)
            }
            case "group.store.search.completed" -> {
                // TODO: EventEnvelope<StoreSearchCompleted> 역직렬화
                // TODO: RealtimePushPort로 방장 화면에 식당 목록 push
                //       (/user/queue/store-search/{voteSessionId})
            }
            default -> log.warn("[vote-result] 알 수 없는 라우팅 키: {}", routingKey);
        }
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
