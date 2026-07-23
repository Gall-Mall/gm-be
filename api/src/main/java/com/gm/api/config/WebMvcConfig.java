package com.gm.api.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gm.api.controller.invite.ratelimit.InviteRateLimitInterceptor;

/**
 * MVC 인터셉터를 등록한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InviteRateLimitInterceptor inviteRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // POST /api/groups/{groupId}/invites(초대 생성)는 방장만 호출하는 저빈도 작업이라
        // 의도적으로 rate limiting 대상에서 제외한다.
        registry.addInterceptor(inviteRateLimitInterceptor).addPathPatterns("/api/invites/**");
    }
}
