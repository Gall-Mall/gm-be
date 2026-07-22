package com.gm.api.auth.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.gm.api.auth.dto.TokenResponse;
import com.gm.api.security.jwt.JwtProvider;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProvider jwtProvider;

    /**
     * 사용자 UUID를 기반으로 Access Token을 발급한다.
     *
     * @param userId 서비스 회원 UUID
     * @return Access Token 응답
     */
    public TokenResponse issue(UUID userId) {
        String accessToken = jwtProvider.createAccessToken(userId);

        return TokenResponse.of(accessToken);
    }
}