package com.gm.api.websocket;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

/**
 * STOMP CONNECT 프레임의 Access Token을 검증해 사용자 식별자를 WebSocket Principal로 등록한다.
 *
 * <p>브라우저 WebSocket API는 HTTP upgrade 요청에 임의의 Authorization 헤더를 넣기 어렵기 때문에,
 * 클라이언트는 STOMP CONNECT native header로 토큰을 전달한다. 이 클래스는 연결 인증만 담당하며
 * 투표 세션 구독 권한은 {@link VoteStompSubscriptionInterceptor}에서 별도로 확인한다.</p>
 */
@Component
@RequiredArgsConstructor
public class VoteStompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    /**
     * CONNECT 프레임의 Bearer Access Token, 로그아웃 폐기 여부와 회원 상태를 검증하고
     * userId 문자열을 이름으로 갖는 Principal을 등록한다.
     * CONNECT가 아닌 프레임은 토큰을 다시 파싱하지 않고 그대로 통과시킨다.
     *
     * @param message 수신한 STOMP 메시지
     * @param channel 메시지가 전달될 inbound channel
     * @return Principal이 등록된 원본 메시지 또는 CONNECT가 아닌 원본 메시지
     * @throws AuthenticationCredentialsNotFoundException Bearer 토큰이 없거나 비어 있는 경우
     * @throws BadCredentialsException Access Token이 폐기됐거나 서명·타입·회원 상태가 유효하지 않은 경우
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket Access Token이 필요합니다.");
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket Access Token이 필요합니다.");
        }

        try {
            Claims claims = jwtProvider.validateAccessToken(token);
            String accessTokenId = jwtProvider.getJti(claims);
            if (accessTokenBlacklistRepository.exists(accessTokenId)) {
                throw new BadCredentialsException("유효하지 않은 WebSocket Access Token입니다.");
            }
            UUID userId = jwtProvider.getUserId(claims);
            User user = userService.findById(userId);
            if (user.status() == UserStatus.WITHDRAWN) {
                throw new BadCredentialsException("유효하지 않은 WebSocket Access Token입니다.");
            }
            // 이후 SUBSCRIBE 프레임은 연결 세션에 저장된 이 Principal을 재사용한다.
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of()
            ));
            return message;
        } catch (JwtException | IllegalArgumentException | UserException exception) {
            throw new BadCredentialsException("유효하지 않은 WebSocket Access Token입니다.", exception);
        }
    }
}
