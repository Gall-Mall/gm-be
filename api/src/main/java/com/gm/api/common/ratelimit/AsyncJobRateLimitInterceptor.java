package com.gm.api.common.ratelimit;

import java.time.Duration;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.CommonException;

/**
 * 유료 외부 API를 호출하는 비동기 작업 요청에 사용자별 요청 빈도 제한을 적용한다.
 *
 * 메뉴 추천(OpenAI)과 식당 검색(Kakao)은 요청 한 번이 유료 호출로 이어진다.
 * 202로 즉시 응답하는 구조라 사용자가 부담 없이 반복 호출할 수 있어(CWE-770),
 * AI 분석 엔드포인트와 같은 방식으로 per-user 고정 윈도우 한도를 둔다.
 */
@Component
public class AsyncJobRateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "ratelimit:async-job:";

    private final RedisFixedWindowRateLimiter rateLimiter;
    private final int perMinute;

    public AsyncJobRateLimitInterceptor(
            RedisFixedWindowRateLimiter rateLimiter,
            @Value("${app.rate-limit.async-job.per-minute:5}") int perMinute
    ) {
        this.rateLimiter = rateLimiter;
        this.perMinute = perMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID userId = resolveUserId();
        if (userId == null) {
            // 이 경로들은 SecurityConfig에서 이미 인증을 요구하므로 정상 흐름에서는 도달하지 않는다.
            return true;
        }

        if (!rateLimiter.tryConsume(KEY_PREFIX + userId, perMinute, Duration.ofMinutes(1))) {
            throw new CommonException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return true;
    }

    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
