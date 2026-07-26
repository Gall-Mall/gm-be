package com.gm.mq.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.gm.core.domain.recommendation.service.MenuRecommendationService;
import com.gm.core.event.payload.SurveyRequested;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import com.gm.mq.event.EventEnvelope;

/**
 * 추천 DLQ 소비자. 재시도가 소진된 세션을 실패로 표시한다.
 *
 * <p>세션은 MENU_RECOMMENDING에서 되돌아갈 상태가 없어, 표시하지 않으면 영구히 막힌다.
 * 사용자는 실패한 세션을 재사용하지 않고 새 세션을 만든다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationDlqListener {

    private final ObjectMapper objectMapper;
    private final MenuRecommendationService menuRecommendationService;

    @RabbitListener(queues = RecommendationQueueConfig.RECOMMENDATION_DLQ)
    public void handle(Message message) {
        SurveyRequested payload = readPayload(message);
        if (payload == null) {
            return;
        }

        try {
            menuRecommendationService.markFailed(payload.voteSessionId());
            log.error("[recommendation] 추천 실패로 세션을 종료한다: session = {}", payload.voteSessionId());
        } catch (RuntimeException e) {
            // 이미 취소·완료된 세션이면 전이가 거부된다. 여기서 더 할 수 있는 일이 없다.
            log.error("[recommendation] 실패 표시 불가: session = {}", payload.voteSessionId(), e);
        }
    }

    /** 본문이 깨졌으면 되살릴 방법이 없다. DLQ에서 또 DLQ로 보내지 않고 로그만 남긴다. */
    private SurveyRequested readPayload(Message message) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(EventEnvelope.class, SurveyRequested.class);
            EventEnvelope<SurveyRequested> envelope = objectMapper.readValue(message.getBody(), type);
            return envelope.payload();
        } catch (RuntimeException e) {
            log.error("[recommendation] DLQ 메시지 해석 실패", e);
            return null;
        }
    }
}
