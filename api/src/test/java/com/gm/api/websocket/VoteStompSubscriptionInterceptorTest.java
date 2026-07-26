package com.gm.api.websocket;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.gm.core.domain.vote.session.service.VoteSubscriptionAuthorizationService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VoteStompSubscriptionInterceptorTest {

    @Test
    @DisplayName("투표 topic의 세션 ID와 CONNECT Principal 사용자 ID로 구독 권한을 확인한다")
    void subscribe_authorizesSessionAndPrincipal() {
        VoteSubscriptionAuthorizationService authorizationService =
                mock(VoteSubscriptionAuthorizationService.class);
        VoteStompSubscriptionInterceptor interceptor =
                new VoteStompSubscriptionInterceptor(authorizationService);
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/vote-sessions/" + sessionId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(
                new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        verify(authorizationService).authorize(sessionId, userId);
    }

    @Test
    @DisplayName("클라이언트가 broker topic으로 직접 SEND하는 요청을 거부한다")
    void send_toBrokerTopic_rejected() {
        VoteSubscriptionAuthorizationService authorizationService =
                mock(VoteSubscriptionAuthorizationService.class);
        VoteStompSubscriptionInterceptor interceptor =
                new VoteStompSubscriptionInterceptor(authorizationService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/vote-sessions/" + UUID.randomUUID());
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(
                new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
