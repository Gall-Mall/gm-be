package com.gm.client.openai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenAI Chat Completions 요청. (/v1/chat/completions)
 *
 * @param model 사용할 모델명
 * @param messages system/user 메시지 목록
 * @param responseFormat JSON mode 지정 ({"type":"json_object"})
 * @param temperature 낮을수록 결정적 출력 (추출 용도라 0 근처)
 * @param maxTokens 출력 토큰 상한 (비용·응답 폭주 방어)
 */
public record OpenAiChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format") Map<String, String> responseFormat,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens
) {

    // 추출 결과 JSON은 짧다. 상한을 넉넉히 두되 무제한 출력은 막는다.
    private static final int EXTRACTION_MAX_TOKENS = 256;

    public record Message(String role, String content) {
    }

    /** JSON mode 추출 요청을 만든다. */
    public static OpenAiChatRequest jsonExtraction(String model, String systemPrompt, String userPrompt) {
        return new OpenAiChatRequest(
                model,
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)
                ),
                Map.of("type", "json_object"),
                0.0,
                EXTRACTION_MAX_TOKENS
        );
    }
}
