package com.gm.api.auth.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.gm.api.auth.dto.TokenResponse;
import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.user.model.UserStatus;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProvider jwtProvider;

    /**
     * 사용자 UUID와 상태를 기반으로 Access Token을 발급한다.
     *
     * @param userId 서비스 회원 UUID
     * @param status 현재 사용자 상태
     * @return Access Token 응답
     */
    public TokenResponse issue(UUID userId, UserStatus status) {
        String accessToken = jwtProvider.createAccessToken(userId, status);

        return TokenResponse.of(accessToken, status);
    }
}