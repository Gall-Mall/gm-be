package com.gm.api.websocket;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class VoteStompAuthenticationInterceptorTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final UserService userService = mock(UserService.class);
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository =
            mock(AccessTokenBlacklistRepository.class);
    private final VoteStompAuthenticationInterceptor interceptor =
            new VoteStompAuthenticationInterceptor(
                    jwtProvider,
                    userService,
                    accessTokenBlacklistRepository
            );

    @Test
    @DisplayName("CONNECT의 Bearer Access Token을 검증해 userId Principal을 등록한다")
    void connect_registersUserIdPrincipal() {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        User user = mock(User.class);
        given(jwtProvider.validateAccessToken("access-token")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(userId);
        given(userService.findById(userId)).willReturn(user);
        given(user.status()).willReturn(UserStatus.ACTIVE);
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer access-token");

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("Authorization 헤더가 없는 CONNECT를 거부한다")
    void connect_withoutAuthorization_rejected() {
        Message<byte[]> message = message(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("만료됐거나 서명이 유효하지 않은 Access Token의 CONNECT를 거부한다")
    void connect_withInvalidToken_rejected() {
        given(jwtProvider.validateAccessToken("invalid-token"))
                .willThrow(new JwtException("invalid"));
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer invalid-token");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("로그아웃으로 blacklist된 Access Token의 CONNECT를 거부한다")
    void connect_withBlacklistedToken_rejected() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.validateAccessToken("blacklisted-token")).willReturn(claims);
        given(jwtProvider.getJti(claims)).willReturn("access-token-id");
        given(accessTokenBlacklistRepository.exists("access-token-id")).willReturn(true);
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer blacklisted-token");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("탈퇴한 회원의 Access Token CONNECT를 거부한다")
    void connect_withWithdrawnUser_rejected() {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        User user = mock(User.class);
        given(jwtProvider.validateAccessToken("withdrawn-token")).willReturn(claims);
        given(jwtProvider.getJti(claims)).willReturn("access-token-id");
        given(jwtProvider.getUserId(claims)).willReturn(userId);
        given(userService.findById(userId)).willReturn(user);
        given(user.status()).willReturn(UserStatus.WITHDRAWN);
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer withdrawn-token");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    private Message<byte[]> message(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
