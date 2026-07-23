package com.gm.api.config;

import com.gm.api.security.oauth.OAuth2FailureHandler;
import com.gm.api.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // OAuth2 로그인 과정에서 인증 요청과 state를 저장할 때만 세션 생성을 허용한다.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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
                        .failureHandler(oauth2FailureHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 네이버 OAuth2 로그인 시작 및 콜백 요청 허용
                        .requestMatchers("/api/auth/oauth/**", "/login/**").permitAll()
                        // 현재 사용자 정보 조회 API는 인증 필요
                        .requestMatchers("/api/users/me").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
