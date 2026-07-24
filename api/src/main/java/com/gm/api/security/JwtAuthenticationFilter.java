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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
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
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = resolveToken(request);

        if (accessToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(accessToken, request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Access Token을 검증하고 Spring Security 인증 정보를 등록한다.
     * ONBOARDING 회원과 ACTIVE 회원은 인증할 수 있다.
     * WITHDRAWN 회원은 인증하지 않는다.
     *
     * @param accessToken Access Token
     * @param request 현재 HTTP 요청
     */
    // 1. JWT 검증
    private void authenticate(String accessToken, HttpServletRequest request) {

        Claims claims = validateAccessToken(accessToken, request);

        if (claims == null) { return; }
        if (isBlacklisted(claims, request)) { return; }

        UUID userId = jwtProvider.getUserId(claims);
        User user = findAvailableUser(userId, request);

        if (user == null) { return; }

        registerAuthentication(userId, user, request);
    }

    private Claims validateAccessToken(String accessToken, HttpServletRequest request) {
        try {

            return jwtProvider.validate(accessToken);
        } catch (JwtException | IllegalArgumentException exception) {

            log.debug(
                    "JWT 검증에 실패했습니다. method={}, path={}, cause={}",
                    request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName()
            );

            return null;
        }
    }

    // 2. 블랙리스트 검사
    private boolean isBlacklisted(Claims claims, HttpServletRequest request) {

        String accessTokenId = jwtProvider.getJti(claims);

        if (!accessTokenBlacklistRepository.exists(accessTokenId)) { return false; }

        log.debug(
                "블랙리스트 Access Token입니다. method={}, path={}, jti={}",
                request.getMethod(), request.getRequestURI(), accessTokenId
        );

        return true;
    }

    // 3. 회원 조회
    private User findAvailableUser(UUID userId, HttpServletRequest request) {

        User user;

        try {

            user = userService.findById(userId);
        } catch (UserException exception) {

            log.debug(
                    "JWT 회원 조회에 실패했습니다. method={}, path={}, userId={}, code={}",
                    request.getMethod(), request.getRequestURI(), userId, exception.getErrorCode().getCode()
            );

            return null;
        }

        if (user.status() == UserStatus.WITHDRAWN) {

            log.debug(
                    "탈퇴한 회원의 JWT 인증 요청입니다. method={}, path={}, userId={}, status={}",
                    request.getMethod(), request.getRequestURI(), userId, user.status()
            );

            return null;
        }

        return user;
    }

    // 4. SecurityContext 등록
    private void registerAuthentication(UUID userId, User user, HttpServletRequest request) {

        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Authorization 헤더에서 Bearer Access Token을 추출한다.
     * @param request 현재 HTTP 요청
     * @return Access Token, 존재하지 않으면 null
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