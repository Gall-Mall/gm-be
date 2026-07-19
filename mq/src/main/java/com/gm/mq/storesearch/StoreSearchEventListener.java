package com.gm.mq.storesearch;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 식당 검색(5번) 도메인 이벤트 소비자.
 *
 * store-search.events.q 소비. store.search.requested → 지도 API 검색 후 완료 이벤트 발행.
 */
@Slf4j
@Component
public class StoreSearchEventListener {

    @RabbitListener(queues = StoreSearchQueueConfig.STORE_SEARCH_QUEUE)
    public void handle(Message message) {
        log.info("[store-search] store.search.requested 수신: {}", new String(message.getBody()));
        // TODO: 멱등(eventId) 체크
        // TODO: EventEnvelope<StoreSearchRequested> 역직렬화
        // TODO: StoreSearchService.search(...) 호출 — 지도 API
        // TODO: 완료 시 group.store.search.completed 발행 (EventPublisher)
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
