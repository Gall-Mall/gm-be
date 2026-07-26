package com.gm.api.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.gm.api.websocket.VoteStompAuthenticationInterceptor;
import com.gm.api.websocket.VoteStompSubscriptionInterceptor;

/**
 * 투표 실시간 이벤트용 STOMP 엔드포인트, 단일 서버 simple broker, inbound 보안 경계를 구성한다.
 * Redis Pub/Sub이나 외부 메시지 브로커를 사용하지 않고 현재 API 인스턴스에서 직접 전송한다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final VoteStompAuthenticationInterceptor authenticationInterceptor;
    private final VoteStompSubscriptionInterceptor subscriptionInterceptor;

    /**
     * 클라이언트가 STOMP 연결을 시작할 WebSocket handshake endpoint를 {@code /ws}로 등록한다.
     * 실제 사용자 인증 정보는 HTTP handshake가 아니라 STOMP CONNECT 헤더에서 검증한다.
     *
     * @param registry STOMP endpoint 등록기
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    /**
     * {@code /topic} destination을 현재 API 서버 메모리의 simple broker가 처리하도록 설정한다.
     *
     * @param registry 메시지 브로커 등록기
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    /**
     * inbound 프레임에 CONNECT JWT 인증을 먼저 적용하고, 이어서 SUBSCRIBE 도메인 인가를 적용한다.
     *
     * @param registration client inbound channel 등록기
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor, subscriptionInterceptor);
    }
}
