package com.gm.mq.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 알림(5번) 도메인 이벤트 소비자.
 *
 * notification.events.q 소비. vote.all.completed → 방장에게 카톡(SOLAPI) 발송.
 */
@Slf4j
@Component
public class NotificationEventListener {

    @RabbitListener(queues = NotificationQueueConfig.NOTIFICATION_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message) {
        log.info("[notification] vote.all.completed 수신: {}", new String(message.getBody()));
        // TODO: 멱등(eventId) 체크 — 이미 처리한 이벤트면 skip
        // TODO: EventEnvelope<VoteAllCompleted> 역직렬화
        // TODO: NotificationService.notifyOwner(groupId, ownerUserId) 호출 — 방장 카톡
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
