package com.gm.mq.recommendation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.gm.core.domain.recommendation.service.MenuRecommendationService;
import com.gm.core.event.EventPublisher;
import com.gm.core.event.payload.SurveyListReady;
import com.gm.core.event.payload.SurveyRequested;
import com.gm.core.transaction.AfterCommitExecutor;
import com.gm.mq.support.EventConsumer;

/**
 * 추천(3번) 도메인 이벤트 소비자.
 *
 * recommendation.events.q 소비. group.survey.requested → 메뉴 후보 생성.
 * 후보 저장과 세션 상태 전이가 있어 inbox 경로를 쓴다.
 */
@Slf4j
@Component
public class RecommendationEventListener {

    private final EventConsumer consumer;
    private final MenuRecommendationService menuRecommendationService;
    private final EventPublisher eventPublisher;
    private final AfterCommitExecutor afterCommitExecutor;

    public RecommendationEventListener(@Qualifier("inboxEventConsumer") EventConsumer consumer,
                                       MenuRecommendationService menuRecommendationService,
                                       EventPublisher eventPublisher,
                                       AfterCommitExecutor afterCommitExecutor) {
        this.consumer = consumer;
        this.menuRecommendationService = menuRecommendationService;
        this.eventPublisher = eventPublisher;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @RabbitListener(queues = RecommendationQueueConfig.RECOMMENDATION_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message) {
        consumer.consumeOnce(message, SurveyRequested.class, payload -> {
            menuRecommendationService.generate(payload.groupId(), payload.voteSessionId());

            log.info("[recommendation] 메뉴 후보 생성 완료: session = {}", payload.voteSessionId());

            // 커밋 전에 발행하면 롤백된 추천의 완료 이벤트가 나간다.
            afterCommitExecutor.execute(() -> eventPublisher.publish(
                    new SurveyListReady(payload.groupId(), payload.voteSessionId())));
        });
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
