package com.gm.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RefreshTokenCookieManager 단위 테스트")
class RefreshTokenCookieManagerTest {

    private static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";
    private static final boolean SECURE = true;
    private static final String SAME_SITE = "Lax";
    private static final long EXPIRATION_SECONDS = 1_209_600L;

    private static final String REFRESH_TOKEN = "refresh-token-value";

    @Nested
    @DisplayName("생성자 설정 검증")
    class ConstructorValidation {

        @Test
        @DisplayName("유효한 설정이면 객체를 생성한다")
        void createsManagerWithValidConfiguration() {
            // when
            RefreshTokenCookieManager manager = createManager();

            // then
            assertThat(manager.getCookieName()).isEqualTo(COOKIE_NAME);
            assertThat(manager.getCookiePath()).isEqualTo(COOKIE_PATH);
            assertThat(manager.isSecure()).isTrue();
            assertThat(manager.getSameSite()).isEqualTo(SAME_SITE);
        }

        @Test
        @DisplayName("쿠키 이름이 null이면 예외가 발생한다")
        void throwsExceptionWhenCookieNameIsNull() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            null,
                            COOKIE_PATH,
                            SECURE,
                            SAME_SITE,
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 이름은 null이거나 공백일 수 없습니다."
                    );
        }

        @Test
        @DisplayName("쿠키 이름이 공백이면 예외가 발생한다")
        void throwsExceptionWhenCookieNameIsBlank() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            "   ",
                            COOKIE_PATH,
                            SECURE,
                            SAME_SITE,
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 이름은 null이거나 공백일 수 없습니다."
                    );
        }

        @Test
        @DisplayName("쿠키 경로가 null이면 예외가 발생한다")
        void throwsExceptionWhenCookiePathIsNull() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            null,
                            SECURE,
                            SAME_SITE,
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 경로는 '/'로 시작해야 합니다."
                    );
        }

        @Test
        @DisplayName("쿠키 경로가 슬래시로 시작하지 않으면 예외가 발생한다")
        void throwsExceptionWhenCookiePathDoesNotStartWithSlash() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            "api/auth",
                            SECURE,
                            SAME_SITE,
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 경로는 '/'로 시작해야 합니다."
                    );
        }

        @Test
        @DisplayName("SameSite가 null이면 예외가 발생한다")
        void throwsExceptionWhenSameSiteIsNull() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            null,
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 SameSite가 설정되지 않았습니다."
                    );
        }

        @Test
        @DisplayName("지원하지 않는 SameSite이면 예외가 발생한다")
        void throwsExceptionWhenSameSiteIsUnsupported() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            "Invalid",
                            EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 SameSite는 Strict, Lax, None 중 하나여야 합니다."
                    );
        }

        @Test
        @DisplayName("SameSite는 대소문자를 구분하지 않는다")
        void acceptsSameSiteIgnoringCase() {
            // when
            RefreshTokenCookieManager strict =
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            "strict",
                            EXPIRATION_SECONDS
                    );

            RefreshTokenCookieManager lax =
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            "LAX",
                            EXPIRATION_SECONDS
                    );

            RefreshTokenCookieManager none =
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            "none",
                            EXPIRATION_SECONDS
                    );

            // then
            assertThat(strict.getSameSite()).isEqualTo("strict");
            assertThat(lax.getSameSite()).isEqualTo("LAX");
            assertThat(none.getSameSite()).isEqualTo("none");
        }

        @Test
        @DisplayName("만료 시간이 0이면 예외가 발생한다")
        void throwsExceptionWhenExpirationIsZero() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            SAME_SITE,
                            0
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 만료 시간은 0보다 커야 합니다."
                    );
        }

        @Test
        @DisplayName("만료 시간이 음수이면 예외가 발생한다")
        void throwsExceptionWhenExpirationIsNegative() {
            assertThatThrownBy(() ->
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            SECURE,
                            SAME_SITE,
                            -1
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 쿠키 만료 시간은 0보다 커야 합니다."
                    );
        }
    }

    @Nested
    @DisplayName("Refresh Token 쿠키 추가")
    class AddRefreshTokenCookie {

        @Test
        @DisplayName("Refresh Token을 HTTP-only 쿠키로 응답에 추가한다")
        void addsRefreshTokenCookie() {
            // given
            RefreshTokenCookieManager manager = createManager();
            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when
            manager.addRefreshTokenCookie(
                    response,
                    REFRESH_TOKEN
            );

            // then
            String setCookie =
                    response.getHeader(HttpHeaders.SET_COOKIE);

            assertThat(setCookie).isNotNull();
            assertThat(setCookie)
                    .contains(COOKIE_NAME + "=" + REFRESH_TOKEN);
            assertThat(setCookie)
                    .contains("Path=" + COOKIE_PATH);
            assertThat(setCookie)
                    .contains("Max-Age=" + EXPIRATION_SECONDS);
            assertThat(setCookie)
                    .contains("Secure");
            assertThat(setCookie)
                    .contains("HttpOnly");
            assertThat(setCookie)
                    .contains("SameSite=" + SAME_SITE);
        }

        @Test
        @DisplayName("secure 설정이 false이면 Secure 속성을 추가하지 않는다")
        void doesNotAddSecureAttributeWhenSecureIsFalse() {
            // given
            RefreshTokenCookieManager manager =
                    new RefreshTokenCookieManager(
                            COOKIE_NAME,
                            COOKIE_PATH,
                            false,
                            SAME_SITE,
                            EXPIRATION_SECONDS
                    );

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when
            manager.addRefreshTokenCookie(
                    response,
                    REFRESH_TOKEN
            );

            // then
            String setCookie =
                    response.getHeader(HttpHeaders.SET_COOKIE);

            assertThat(setCookie).isNotNull();
            assertThat(setCookie).doesNotContain("Secure");
            assertThat(setCookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("HTTP 응답이 null이면 예외가 발생한다")
        void throwsExceptionWhenResponseIsNull() {
            RefreshTokenCookieManager manager = createManager();

            assertThatThrownBy(() ->
                    manager.addRefreshTokenCookie(
                            null,
                            REFRESH_TOKEN
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "HTTP 응답은 null일 수 없습니다."
                    );
        }

        @Test
        @DisplayName("Refresh Token이 null이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsNull() {
            RefreshTokenCookieManager manager = createManager();
            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            assertThatThrownBy(() ->
                    manager.addRefreshTokenCookie(
                            response,
                            null
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token은 null이거나 공백일 수 없습니다."
                    );

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                    .isNull();
        }

        @Test
        @DisplayName("Refresh Token이 공백이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsBlank() {
            RefreshTokenCookieManager manager = createManager();
            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            assertThatThrownBy(() ->
                    manager.addRefreshTokenCookie(
                            response,
                            "   "
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token은 null이거나 공백일 수 없습니다."
                    );

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Refresh Token 쿠키 조회")
    class ResolveRefreshToken {

        @Test
        @DisplayName("요청 쿠키에서 Refresh Token을 조회한다")
        void resolvesRefreshToken() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setCookies(
                    new Cookie("other-cookie", "other-value"),
                    new Cookie(COOKIE_NAME, REFRESH_TOKEN)
            );

            // when
            Optional<String> result =
                    manager.resolveRefreshToken(request);

            // then
            assertThat(result)
                    .contains(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("동일한 이름의 쿠키가 여러 개이면 값이 있는 첫 번째 쿠키를 반환한다")
        void resolvesFirstNonBlankMatchingCookie() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setCookies(
                    new Cookie(COOKIE_NAME, ""),
                    new Cookie(COOKIE_NAME, "first-token"),
                    new Cookie(COOKIE_NAME, "second-token")
            );

            // when
            Optional<String> result =
                    manager.resolveRefreshToken(request);

            // then
            assertThat(result).contains("first-token");
        }

        @Test
        @DisplayName("요청이 null이면 빈 Optional을 반환한다")
        void returnsEmptyWhenRequestIsNull() {
            RefreshTokenCookieManager manager = createManager();

            assertThat(manager.resolveRefreshToken(null))
                    .isEmpty();
        }

        @Test
        @DisplayName("요청에 쿠키가 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenCookiesAreNull() {
            // given
            RefreshTokenCookieManager manager = createManager();
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            // when
            Optional<String> result =
                    manager.resolveRefreshToken(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenRefreshTokenCookieDoesNotExist() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setCookies(
                    new Cookie("other-cookie", "other-value")
            );

            // when
            Optional<String> result =
                    manager.resolveRefreshToken(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Refresh Token 쿠키 값이 공백이면 빈 Optional을 반환한다")
        void returnsEmptyWhenRefreshTokenCookieValueIsBlank() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setCookies(
                    new Cookie(COOKIE_NAME, "   ")
            );

            // when
            Optional<String> result =
                    manager.resolveRefreshToken(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 존재하면 문자열로 반환한다")
        void resolvesRefreshTokenOrNull() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setCookies(
                    new Cookie(COOKIE_NAME, REFRESH_TOKEN)
            );

            // when
            String result =
                    manager.resolveRefreshTokenOrNull(request);

            // then
            assertThat(result).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 null을 반환한다")
        void returnsNullWhenRefreshTokenDoesNotExist() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            // when
            String result =
                    manager.resolveRefreshTokenOrNull(request);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Refresh Token 쿠키 삭제")
    class DeleteRefreshTokenCookie {

        @Test
        @DisplayName("동일한 이름과 경로를 가진 만료 쿠키를 응답에 추가한다")
        void deletesRefreshTokenCookie() {
            // given
            RefreshTokenCookieManager manager = createManager();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when
            manager.deleteRefreshTokenCookie(response);

            // then
            String setCookie =
                    response.getHeader(HttpHeaders.SET_COOKIE);

            assertThat(setCookie).isNotNull();
            assertThat(setCookie)
                    .contains(COOKIE_NAME + "=");
            assertThat(setCookie)
                    .contains("Path=" + COOKIE_PATH);
            assertThat(setCookie)
                    .contains("Max-Age=0");
            assertThat(setCookie)
                    .contains("Secure");
            assertThat(setCookie)
                    .contains("HttpOnly");
            assertThat(setCookie)
                    .contains("SameSite=" + SAME_SITE);
        }

        @Test
        @DisplayName("HTTP 응답이 null이면 예외가 발생한다")
        void throwsExceptionWhenResponseIsNull() {
            RefreshTokenCookieManager manager = createManager();

            assertThatThrownBy(() ->
                    manager.deleteRefreshTokenCookie(null)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "HTTP 응답은 null일 수 없습니다."
                    );
        }
    }

    private RefreshTokenCookieManager createManager() {
        return new RefreshTokenCookieManager(
                COOKIE_NAME,
                COOKIE_PATH,
                SECURE,
                SAME_SITE,
                EXPIRATION_SECONDS
        );
    }
}