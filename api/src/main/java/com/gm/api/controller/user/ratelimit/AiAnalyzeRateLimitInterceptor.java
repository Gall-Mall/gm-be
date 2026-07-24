package com.gm.api.controller.user.ratelimit;

import java.time.Duration;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gm.api.common.ratelimit.RedisFixedWindowRateLimiter;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.CommonException;

/**
 * AI 분석 엔드포인트({@code /api/users/me/*&#47;analyze})에 사용자별 요청 빈도 제한을 적용한다.
 *
 * 알레르기·음식 취향 분석은 매 요청이 외부 유료 LLM(OpenAI)을 호출한다. 인증만으로는
 * 계정당 무제한 반복 호출을 막지 못하므로(CWE-770), per-user 고정 윈도우 한도를 둔다.
 * 인증 사용자 식별자는 {@link SecurityContextHolder}에서 읽는다(이 경로는 이미 인증을 요구한다).
 */
@Component
public class AiAnalyzeRateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "ratelimit:ai-analyze:";

    private final RedisFixedWindowRateLimiter rateLimiter;
    private final int perMinute;

    public AiAnalyzeRateLimitInterceptor(
            RedisFixedWindowRateLimiter rateLimiter,
            @Value("${app.rate-limit.ai-analyze.per-minute:5}") int perMinute
    ) {
        this.rateLimiter = rateLimiter;
        this.perMinute = perMinute;
    }

    /**
     * 사용자별 분당 한도를 확인한다. 한도를 넘으면
     * {@link CommonErrorCode#RATE_LIMIT_EXCEEDED}로 즉시 요청을 거절한다.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID userId = resolveUserId();
        if (userId == null) {
            // 이 경로는 SecurityConfig에서 이미 인증을 요구하므로 정상 흐름에서는 도달하지 않는다.
            return true;
        }

        if (!rateLimiter.tryConsume(KEY_PREFIX + userId, perMinute, Duration.ofMinutes(1))) {
            throw new CommonException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return true;
    }

    /** SecurityContext에 인증된 {@link CustomUserPrincipal}이 있으면 그 userId를 반환한다. */
    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
