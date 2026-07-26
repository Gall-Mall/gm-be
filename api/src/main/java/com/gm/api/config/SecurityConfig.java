package com.gm.api.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gm.api.security.*;
import com.gm.api.security.oauth.CustomOAuth2UserService;
import com.gm.api.security.oauth.OAuth2FailureHandler;
import com.gm.api.security.oauth.OAuth2SuccessHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 네이버 OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 네이버 로그인 시작 경로
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/api/auth/oauth"))
                        // 네이버 로그인 완료 후 콜백 경로
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/api/auth/oauth/*/callback"))
                        // 네이버 사용자 정보 조회 및 회원 처리
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 네이버 OAuth2 로그인 시작 및 콜백 요청 허용
                        .requestMatchers("/api/auth/oauth/**", "/login/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/token", "/api/auth/token/refresh").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers("/api/groups/**").authenticated()
                        .requestMatchers("/api/invites/**").authenticated()
                        .anyRequest().authenticated())
                //JwtAuthenticationFilter를 Spring Security의 UsernamePasswordAuthenticationFilter보다 먼저 실행한다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
