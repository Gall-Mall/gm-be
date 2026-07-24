package com.gm.api.config;

import com.gm.api.security.oauth.OAuth2FailureHandler;
import com.gm.api.security.oauth.OAuth2SuccessHandler;
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
                        // OAuth2 로그인 성공 처리
                        .successHandler(oauth2SuccessHandler)
                        // OAuth2 로그인 실패 처리
                        .failureHandler(oauth2FailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 네이버 OAuth2 로그인 시작 및 콜백 요청 허용
                        .requestMatchers("/api/auth/oauth/**", "/login/**").permitAll()
                        /*
                         * 로그인 교환 코드와 Refresh Token 쿠키를 이용하는 API다.
                         * 아직 Access Token이 발급되지 않은 상태에서 호출하므로 Spring Security 인증을 요구하면 안 된다.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/auth/token", "/api/auth/token/refresh").permitAll()
                        /*
                         * 로그아웃은 현재 Access Token으로 인증된 사용자만 호출할 수 있도록 설정한다.
                         * JwtAuthenticationFilter가 Access Token을 검증한 뒤 SecurityContext에 인증 정보를 등록한다.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        // "/api/users/me/onboarding"과 음식 설정 하위 API까지 인증이 필요하다.
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        // Group 도메인은 JWT 인증이 필요하다.
                        .requestMatchers("/api/groups/**").authenticated()
                        // Invite 도메인은 JWT 인증이 필요하다.
                        .requestMatchers("/api/invites/**").authenticated()
                        .anyRequest().permitAll())
                //JwtAuthenticationFilter를 Spring Security의 UsernamePasswordAuthenticationFilter보다 먼저 실행한다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
