package com.gm.client.openai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gm.client.openai.dto.OpenAiChatRequest;
import com.gm.client.openai.dto.OpenAiChatResponse;
import com.gm.client.openai.exception.OpenAiApiException;
import com.gm.client.openai.exception.OpenAiErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * OpenAI Chat Completions(JSON mode) 호출을 담당하는 공용 클라이언트.
 *
 * 여러 어댑터(알레르기·음식 추출, 메뉴 큐레이션)가 동일한 HTTP 호출을 공유한다.
 * 응답 본문(JSON 문자열) 반환까지만 책임지고, 도메인 파싱은 각 어댑터가 한다.
 */
@Component
@RequiredArgsConstructor
public class OpenAiJsonClient {

    private final RestClient openAiRestClient;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    /** JSON mode로 호출하고 응답 본문 JSON 문자열을 반환한다. */
    public String requestJson(String systemPrompt, String userPrompt, int maxTokens) {
        OpenAiChatResponse response;
        try {
            response = openAiRestClient
                    .post()
                    .uri("/v1/chat/completions")
                    .body(OpenAiChatRequest.jsonMode(model, systemPrompt, userPrompt, maxTokens))
                    .retrieve()
                    .body(OpenAiChatResponse.class);
        } catch (RestClientException e) {
            throw new OpenAiApiException(OpenAiErrorCode.OPENAI_API_ERROR);
        }

        String content = response == null ? null : response.firstContent();
        if (content == null) {
            throw new OpenAiApiException(OpenAiErrorCode.OPENAI_RESPONSE_ERROR);
        }
        return content;
    }
}
