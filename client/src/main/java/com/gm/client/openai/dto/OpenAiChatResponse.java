package com.gm.client.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OpenAI Chat Completions 응답. 필요한 필드만 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiChatResponse(
        List<Choice> choices
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {
    }

    /** 첫 번째 choice의 본문을 꺼낸다. 없으면 null. */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice first = choices.get(0);
        return first.message() == null ? null : first.message().content();
    }
}
