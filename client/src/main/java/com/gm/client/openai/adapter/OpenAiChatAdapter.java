package com.gm.client.openai.adapter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.client.openai.dto.OpenAiChatRequest;
import com.gm.client.openai.dto.OpenAiChatResponse;
import com.gm.client.openai.dto.PreferenceExtractionJson;
import com.gm.client.openai.exception.OpenAiApiException;
import com.gm.client.openai.exception.OpenAiErrorCode;
import com.gm.core.domain.user.port.AiChatPort;
import com.gm.core.domain.user.model.ExtractedPreference;

import lombok.RequiredArgsConstructor;

/**
 * OpenAI Chat Completions(JSON mode) 기반 선호 추출 어댑터.
 *
 * <p>표준 알레르기 후보 목록을 프롬프트에 함께 실어 보내고, AI는 그 목록 안의 이름만
 * 반환하도록 지시한다. 응답 검증(화이트리스트)은 core 서비스가 담당한다.</p>
 */
@RequiredArgsConstructor
@Component
public class OpenAiChatAdapter implements AiChatPort {

    private static final String SYSTEM_PROMPT = """
            너는 음식 취향 분석기다. 사용자의 자유텍스트에서 알레르기와 음식 호불호를 추출한다.
            반드시 아래 JSON 형식으로만 응답한다.
            {
              "standard_allergens": ["제공된 표준 알레르기 목록에 있는 이름만, 정확히 그 표기 그대로"],
              "custom_allergens": ["알레르기이지만 표준 목록에 없는 항목 (음식/성분 이름만 간결하게)"],
              "preference_text": "좋아한다고 언급한 음식·취향 요약 (없으면 빈 문자열)",
              "exclude_food_text": "싫어한다고 언급한 음식 요약 (없으면 빈 문자열)"
            }
            규칙:
            - standard_allergens에는 제공된 목록에 없는 이름을 절대 넣지 않는다.
            - 알레르기가 아닌 단순 불호는 exclude_food_text로 분류한다.
            - 텍스트에 없는 내용을 지어내지 않는다.
            """;

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public ExtractedPreference extractPreference(String freeText, List<String> standardAllergenNames) {

        String userPrompt = """
                [표준 알레르기 목록]
                %s

                [사용자 입력]
                %s
                """.formatted(String.join(", ", standardAllergenNames), freeText);

        OpenAiChatResponse response;
        try {
            response = openAiRestClient
                    .post()
                    .uri("/v1/chat/completions")
                    .body(OpenAiChatRequest.jsonExtraction(model, SYSTEM_PROMPT, userPrompt))
                    .retrieve()
                    .body(OpenAiChatResponse.class);
        } catch (RestClientException e) {
            throw new OpenAiApiException(OpenAiErrorCode.OPENAI_API_ERROR);
        }

        String content = response == null ? null : response.firstContent();
        if (content == null) {
            throw new OpenAiApiException(OpenAiErrorCode.OPENAI_RESPONSE_ERROR);
        }

        try {
            return objectMapper.readValue(content, PreferenceExtractionJson.class).toDomain();
        } catch (JacksonException e) {
            throw new OpenAiApiException(OpenAiErrorCode.OPENAI_RESPONSE_ERROR);
        }
    }
}
