package com.gm.client.kakao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoClientConfig {

    @Value("${kakao.local.key}")
    private String apiKey;

    @Bean
    public RestClient restClient() {

        return RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }
}


