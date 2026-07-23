package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverOAuth2UserInfoTest {

    @Test
    @DisplayName("네이버 response 속성에서 사용자 정보를 추출한다")
    void extractsNaverUserInformation() {
        // given
        Map<String, Object> attributes = Map.of(
                "resultcode", "00",
                "message", "success",
                "response", Map.of(
                        "id", "naver-provider-id",
                        "name", "홍길동",
                        "email", "user@example.com",
                        "mobile", "010-1234-5678"
                )
        );

        // when
        NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

        // then
        assertThat(userInfo.providerId()).isEqualTo("naver-provider-id");
        assertThat(userInfo.name()).isEqualTo("홍길동");
        assertThat(userInfo.email()).isEqualTo("user@example.com");
        assertThat(userInfo.phone()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("네이버 응답에 response가 없으면 예외가 발생한다")
    void throwsWhenResponseDoesNotExist() {
        Map<String, Object> attributes = Map.of(
                "resultcode", "00",
                "message", "success"
        );

        assertThatThrownBy(() -> new NaverOAuth2UserInfo(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("네이버 사용자 정보(response)가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("네이버 response가 Map 형식이 아니면 예외가 발생한다")
    void throwsWhenResponseIsNotMap() {
        Map<String, Object> attributes = Map.of("response", "invalid-response");

        assertThatThrownBy(() -> new NaverOAuth2UserInfo(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("네이버 사용자 정보(response)가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("네이버 사용자 식별자가 없으면 예외가 발생한다")
    void throwsWhenProviderIdDoesNotExist() {
        Map<String, Object> attributes = Map.of(
                "response", Map.of(
                        "name", "홍길동",
                        "email", "user@example.com",
                        "mobile", "010-1234-5678"
                )
        );

        NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

        assertThatThrownBy(userInfo::providerId)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("네이버 사용자 식별자가 존재하지 않습니다.");
    }
}