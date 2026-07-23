package com.gm.api.security.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate;
    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        // 1. 네이버 사용자 정보 API 호출
        OAuth2User oauth2User = delegate.loadUser(request);

        // 2. 사용자 정보 파싱
        NaverOAuth2UserInfo userInfo;
        try {
            userInfo = new NaverOAuth2UserInfo(oauth2User.getAttributes());
        } catch (IllegalArgumentException e) {
            log.warn("네이버 사용자 정보 파싱 실패", e);

            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"), "네이버 사용자 정보를 처리할 수 없습니다.", e);
        }

        // 3. 기존 회원 조회 또는 신규 회원 생성 : 회원 UUID와 도메인 User를 함께 반환받는다.
        UserResult result;
        try {
            result = userService.findOrCreateWithId(
                    userInfo.name(),
                    Provider.NAVER,
                    userInfo.providerId(),
                    userInfo.phone(),
                    userInfo.email());
        } catch (RuntimeException e) {
            log.error("회원 조회/생성 중 서버 오류", e);

            throw new AuthenticationServiceException("회원 정보를 처리하는 중 오류가 발생했습니다.", e);
        }

        // 4. 로그인 성공 로그 : 개인정보인 이름, 이메일, 전화번호는 로그에 남기지 않는다.
        log.info("네이버 OAuth2 로그인 성공: userId={}, provider={}", result.userId(), Provider.NAVER);

        // 5. Spring Security가 사용할 Principal 반환
        return new CustomUserPrincipal(result.userId(), result.user(), oauth2User.getAttributes());
    }
}