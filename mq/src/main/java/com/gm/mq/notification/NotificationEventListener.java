package com.gm.mq.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.gm.core.event.payload.VoteAllCompleted;
import com.gm.mq.support.EventConsumer;

/**
 * 알림(5번) 도메인 이벤트 소비자.
 *
 * notification.events.q 소비. vote.all.completed → 방장에게 카톡(SOLAPI) 발송.
 *
 * <p>DB를 쓰지 않아 dedup-only 경로다. 이 경로의 실제 위험은 중복 발송이 아니라
 * <b>미발송</b>이다 — 기록 직후 프로세스가 죽으면 재전달분이 스킵되어 카톡이 아예 안 나간다.
 * 발송 자체를 보장하려면 "발송 완료"를 DB 트랜잭션에 묶는 inbox 경로로 옮겨야 한다.
 * NotificationService를 만들 때 함께 결정할 것.</p>
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final EventConsumer consumer;

    public NotificationEventListener(@Qualifier("dedupOnlyEventConsumer") EventConsumer consumer) {
        this.consumer = consumer;
    }

    @RabbitListener(queues = NotificationQueueConfig.NOTIFICATION_QUEUE, errorHandler = "mqListenerErrorHandler")
    public void handle(Message message) {
        consumer.consumeOnce(message, VoteAllCompleted.class, payload -> {
            log.info("[notification] vote.all.completed 수신: {}", payload.voteSessionId());
            // TODO: NotificationService.notifyOwner(groupId, ownerUserId) 호출 — 방장 카톡
        });
        // 예외는 삼키지 말 것 (throw → 재시도 → DLQ)
    }
}
