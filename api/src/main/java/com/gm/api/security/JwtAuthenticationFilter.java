package com.gm.api.security;

import java.io.IOException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = resolveToken(request);

        if (accessToken != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(accessToken, request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Access Token을 검증하고 Spring Security 인증 정보를 등록한다.
     */
    private void authenticate(String accessToken, HttpServletRequest request) {
        UUID userId;

        try {
            // 1. Access Token 검증 및 회원 UUID 추출
            userId = jwtProvider.validateAndGetUserId(accessToken);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT 검증에 실패했습니다. method={}, path={}, cause={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName()
            );

            return;
        }

        User user;

        try {
            // 2. 회원 조회
            user = userService.findById(userId);
        } catch (UserException exception) {
            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT 회원 조회에 실패했습니다. method={}, path={}, userId={}, code={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    userId,
                    exception.getErrorCode().getCode()
            );

            return;
        }

        // 3. 활성 상태 회원인지 확인
        if (user.status() != UserStatus.ACTIVE) {
            log.debug(
                    "비활성 상태 회원의 JWT 인증 요청입니다. method={}, path={}, userId={}, status={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    userId,
                    user.status()
            );

            return;
        }

        // 4. 인증 Principal 생성
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);

        // 5. 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,null, principal.getAuthorities()
                );

        // 6. 요청 세부 정보 설정
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 7. SecurityContext에 인증 정보 등록
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Authorization 헤더에서 Bearer Access Token을 추출한다.
     */
    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)) { return null; }
        if (!authorizationHeader.startsWith(TOKEN_PREFIX)) { return null; }

        String accessToken = authorizationHeader.substring(TOKEN_PREFIX.length());

        if (!StringUtils.hasText(accessToken)) { return null; }

        return accessToken;
    }
}