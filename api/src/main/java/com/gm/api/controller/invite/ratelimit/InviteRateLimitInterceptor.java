package com.gm.api.controller.invite.ratelimit;

import java.time.Duration;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gm.api.common.ratelimit.RedisFixedWindowRateLimiter;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.CommonException;

/**
 * {@code /api/invites/**} 경로에 사용자별 요청 빈도 제한을 적용한다.
 *
 * <p>초대 코드는 6자리라 엔트로피가 낮으므로(약 35.7비트), TTL만으로는 브루트포스를 막기
 * 부족해 rate limiting을 추가 방어선으로 둔다. 조회(GET)는 분당 한도만, 가입(POST)은
 * 분당·시간당 두 한도를 모두 적용한다 — 두 한도 모두 통과해야 요청이 진행된다.</p>
 *
 * <p>인증된 사용자 식별자는 {@link SecurityContextHolder}에서 읽는다. 인터셉터는 Spring
 * Security 필터체인 이후 실행되므로, 이 경로가 {@code SecurityConfig}에서 인증을 요구하는 한
 * 이 시점엔 이미 인증 정보가 채워져 있다.</p>
 */
@Component
public class InviteRateLimitInterceptor implements HandlerInterceptor {

    private static final String INFO_KEY_PREFIX = "ratelimit:invite:info:";
    private static final String JOIN_MINUTE_KEY_PREFIX = "ratelimit:invite:join:min:";
    private static final String JOIN_HOUR_KEY_PREFIX = "ratelimit:invite:join:hour:";

    private final RedisFixedWindowRateLimiter rateLimiter;
    private final int infoPerMinute;
    private final int joinPerMinute;
    private final int joinPerHour;

    public InviteRateLimitInterceptor(
            RedisFixedWindowRateLimiter rateLimiter,
            @Value("${app.rate-limit.invite.info-per-minute:20}") int infoPerMinute,
            @Value("${app.rate-limit.invite.join-per-minute:10}") int joinPerMinute,
            @Value("${app.rate-limit.invite.join-per-hour:30}") int joinPerHour
    ) {
        this.rateLimiter = rateLimiter;
        this.infoPerMinute = infoPerMinute;
        this.joinPerMinute = joinPerMinute;
        this.joinPerHour = joinPerHour;
    }

    /**
     * 요청 메서드에 따라 조회·가입 한도를 확인한다. 한도를 넘으면
     * {@link CommonErrorCode#RATE_LIMIT_EXCEEDED}로 즉시 요청을 거절한다.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID userId = resolveUserId();
        if (userId == null) {
            // 이 경로는 SecurityConfig에서 이미 인증을 요구하므로 정상 흐름에서는 도달하지 않는다.
            return true;
        }

        if (HttpMethod.GET.matches(request.getMethod())) {
            check(INFO_KEY_PREFIX + userId, infoPerMinute, Duration.ofMinutes(1));
        } else if (HttpMethod.POST.matches(request.getMethod())) {
            check(JOIN_MINUTE_KEY_PREFIX + userId, joinPerMinute, Duration.ofMinutes(1));
            check(JOIN_HOUR_KEY_PREFIX + userId, joinPerHour, Duration.ofHours(1));
        }

        return true;
    }

    private void check(String key, int limit, Duration window) {
        if (!rateLimiter.tryConsume(key, limit, window)) {
            throw new CommonException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
        }
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
