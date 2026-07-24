package com.gm.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 로그인 완료 후 Access Token 교환에 사용하는 요청이다.
 *
 * @param code OAuth2SuccessHandler가 발급한 일회용 로그인 교환 코드
 */
public record LoginExchangeRequest(@NotBlank(message = "로그인 교환 코드는 필수입니다.") String code) {}