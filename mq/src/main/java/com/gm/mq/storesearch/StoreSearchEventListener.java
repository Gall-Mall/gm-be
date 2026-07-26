package com.gm.mq.storesearch;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.gm.core.domain.store.StoreSearchService;
import com.gm.core.event.EventPublisher;
import com.gm.core.event.payload.StoreSearchCompleted;
import com.gm.core.event.payload.StoreSearchRequested;
import com.gm.core.transaction.AfterCommitExecutor;
import com.gm.mq.support.EventConsumer;

/**
 * 식당 검색(5번) 도메인 이벤트 소비자.
 *
 * store-search.events.q 소비. store.search.requested → 지도 API 검색 후 완료 이벤트 발행.
 * 식당 저장과 세션 상태 전이가 있어 inbox 경로를 쓴다.
 */
@Slf4j
@Component
public class StoreSearchEventListener {

    private final EventConsumer consumer;
    private final StoreSearchService storeSearchService;
    private final EventPublisher eventPublisher;
    private final AfterCommitExecutor afterCommitExecutor;

    public StoreSearchEventListener(@Qualifier("inboxEventConsumer") EventConsumer consumer,
                                    StoreSearchService storeSearchService,
                                    EventPublisher eventPublisher,
                                    AfterCommitExecutor afterCommitExecutor) {
        this.consumer = consumer;
        this.storeSearchService = storeSearchService;
        this.eventPublisher = eventPublisher;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @RabbitListener(queues = StoreSearchQueueConfig.STORE_SEARCH_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message) {
        consumer.consumeOnce(message, StoreSearchRequested.class, payload -> {
            var stores = storeSearchService.searchAndSave(
                    payload.voteSessionId(),
                    payload.keyword(),
                    payload.center(),
                    payload.radius()
            );

            log.info("[store-search] 검색 완료: session = {}, 건수 = {}",
                    payload.voteSessionId(), stores.size());

            // 커밋 전에 발행하면 롤백된 검색의 완료 이벤트가 나간다.
            afterCommitExecutor.execute(() ->
                    eventPublisher.publish(new StoreSearchCompleted(payload.voteSessionId())));
        });
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
