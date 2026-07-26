package com.gm.api.websocket;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.gm.core.domain.vote.session.service.VoteSubscriptionAuthorizationService;

/**
 * 투표 세션 topic 구독을 해당 세션 그룹의 활성 멤버에게만 허용한다.
 *
 * <p>CONNECT 단계에서 등록된 userId Principal을 신뢰하되, destination의 세션 존재 여부와
 * 세션 소속 그룹의 ACTIVE 멤버 여부는 구독할 때마다 Core 인가 서비스에서 확인한다.
 * 따라서 유효한 JWT를 가진 사용자라도 다른 그룹 세션은 구독할 수 없다.</p>
 */
@Component
@RequiredArgsConstructor
public class VoteStompSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(
            "^/topic/vote-sessions/([0-9a-fA-F-]{36})$"
    );

    private final VoteSubscriptionAuthorizationService authorizationService;

    /**
     * SUBSCRIBE destination에서 voteSessionId를 추출하고 Principal 사용자의 도메인 구독 권한을 확인한다.
     * 클라이언트 SEND는 서버 전용 topic 이벤트를 위조할 수 있으므로 전면 거부한다.
     *
     * @param message 수신한 STOMP 메시지
     * @param channel 메시지가 전달될 inbound channel
     * @return 구독이 허용된 원본 메시지 또는 인증·구독과 무관한 원본 메시지
     * @throws AccessDeniedException SEND 요청이거나 destination 형식, Principal, UUID 형식 또는 그룹 멤버 권한이 유효하지 않은 경우
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (accessor != null && accessor.getCommand() == StompCommand.SEND) {
            // /topic은 서버 publisher 전용이며 클라이언트가 직접 이벤트를 방송할 수 없다.
            throw new AccessDeniedException("클라이언트 WebSocket SEND는 허용되지 않습니다.");
        }
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        // 허용된 topic 형식을 정확히 일치시켜 임의 broker destination 구독을 막는다.
        Matcher matcher = DESTINATION_PATTERN.matcher(String.valueOf(accessor.getDestination()));
        Principal principal = accessor.getUser();
        if (!matcher.matches() || principal == null) {
            throw new AccessDeniedException("허용되지 않은 투표 구독입니다.");
        }

        try {
            // CONNECT 인증과 분리된 Core 도메인 인가에서 세션 소속과 ACTIVE 멤버십을 확인한다.
            authorizationService.authorize(
                    UUID.fromString(matcher.group(1)),
                    UUID.fromString(principal.getName())
            );
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("허용되지 않은 투표 구독입니다.", exception);
        }
        return message;
    }
}
