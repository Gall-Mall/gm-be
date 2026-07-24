package com.gm.client.openai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenAI Chat Completions 요청. (/v1/chat/completions)
 * responseFormat으로 JSON mode를 지정하고, maxTokens로 출력 상한을 둔다.
 */
public record OpenAiChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format") Map<String, String> responseFormat,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens
) {

    public record Message(String role, String content) {
    }

    /** JSON mode 요청을 만든다. maxTokens로 출력 상한을 지정한다(비용·폭주 방어). */
    public static OpenAiChatRequest jsonMode(String model, String systemPrompt, String userPrompt, int maxTokens) {
        return new OpenAiChatRequest(
                model,
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)
                ),
                Map.of("type", "json_object"),
                0.0,
                maxTokens
        );
    }
}
