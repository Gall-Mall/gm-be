package com.gm.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2AuthorizationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("네이버 OAuth 로그인 시작 요청 시 네이버 인증 페이지로 리다이렉트한다")
    void oauthAuthorization_redirectsToNaver() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/naver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        header().string(
                                "Location",
                                org.hamcrest.Matchers.containsString("nid.naver.com/oauth2.0/authorize")
                        ))
                .andExpect(
                        header().string(
                                "Location",
                                org.hamcrest.Matchers.containsString("client_id=")
                        ))
                .andExpect(
                        header().string(
                                "Location", org.hamcrest.Matchers.containsString("response_type=code")));
    }
}