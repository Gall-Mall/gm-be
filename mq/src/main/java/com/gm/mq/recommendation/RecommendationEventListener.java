package com.gm.mq.recommendation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 추천(3번) 도메인 이벤트 소비자.
 *
 * recommendation.events.q 소비 — 이벤트 2종을 라우팅 키로 분기.
 * user.onboarding.submitted → AI 취향 분석 / group.survey.requested → 메뉴 후보 생성.
 */
@Slf4j
@Component
public class RecommendationEventListener {

    @RabbitListener(queues = RecommendationQueueConfig.RECOMMENDATION_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("[recommendation] {} 수신: {}", routingKey, new String(message.getBody()));
        // TODO: 멱등(eventId) 체크
        switch (routingKey) {
            case "user.onboarding.submitted" -> {
                // TODO: EventEnvelope<OnboardingSubmitted> 역직렬화
                // TODO: RecommendationService.analyzeOnboarding(...) — AI 취향 분석 후 프로필 저장
            }
            case "group.survey.requested" -> {
                // TODO: EventEnvelope<SurveyRequested> 역직렬화
                // TODO: RecommendationService.generateMenuCandidates(...) — 메뉴 후보 ~30개 생성
                // TODO: 완료 시 group.survey.list.ready 발행 (EventPublisher)
            }
            default -> log.warn("[recommendation] 알 수 없는 라우팅 키: {}", routingKey);
        }
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
