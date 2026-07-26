package com.gm.api.websocket;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.gm.core.domain.vote.event.VoteSocketEvent;
import com.gm.core.domain.vote.event.VoteSocketEventPublisher;

/**
 * Core publisher port로 전달된 투표 이벤트를 단일 API 서버의 STOMP simple broker로 직접 전송한다.
 * Core는 Spring Messaging 타입을 알지 못하며, 이 API adapter만 {@link SimpMessagingTemplate}에 의존한다.
 */
@Component
@RequiredArgsConstructor
public class StompVoteSocketEventPublisher implements VoteSocketEventPublisher {

    private static final String DESTINATION_PREFIX = "/topic/vote-sessions/";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 이벤트의 voteSessionId를 사용해 세션 전용 topic으로 공통 payload를 전송한다.
     *
     * @param event eventType, voteSessionId, occurredAt, 이벤트별 data를 포함한 Core 이벤트
     */
    @Override
    public void publish(VoteSocketEvent event) {
        messagingTemplate.convertAndSend(
                DESTINATION_PREFIX + event.voteSessionId(),
                event
        );
    }
}
