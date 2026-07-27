package com.gm.api.config;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import com.gm.api.websocket.VoteStompAuthenticationInterceptor;
import com.gm.api.websocket.VoteStompSubscriptionInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    @DisplayName("WebSocket handshake는 설정된 프론트엔드 origin만 허용한다")
    void registersOnlyConfiguredFrontendOrigin() {
        VoteStompAuthenticationInterceptor authenticationInterceptor =
                mock(VoteStompAuthenticationInterceptor.class);
        VoteStompSubscriptionInterceptor subscriptionInterceptor =
                mock(VoteStompSubscriptionInterceptor.class);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration =
                mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);

        WebSocketConfig config = new WebSocketConfig(
                authenticationInterceptor,
                subscriptionInterceptor,
                List.of("http://localhost:5173")
        );

        config.registerStompEndpoints(registry);

        verify(registration).setAllowedOrigins("http://localhost:5173");
        verify(registration, never()).setAllowedOriginPatterns(any(String[].class));
    }
}
