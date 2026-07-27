package com.gm.client.openai.adapter;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import com.gm.client.openai.client.OpenAiJsonClient;
import com.gm.core.domain.recommendation.model.MenuCurationCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpenAiMenuCurationAdapterTest {

    @Mock
    private OpenAiJsonClient openAiJsonClient;

    @Test
    void prioritizesMarkedVoteSessionKeywordsInPrompt() {
        given(openAiJsonClient.requestJson(anyString(), anyString(), anyInt()))
                .willReturn("{\"menus\":[]}");
        OpenAiMenuCurationAdapter adapter =
                new OpenAiMenuCurationAdapter(openAiJsonClient, new ObjectMapper());

        adapter.curate(new MenuCurationCommand(
                List.of("김치찌개", "잔치국수"),
                List.of(),
                List.of("오늘의 핵심 선호: 매콤한, 국물", "든든한 음식"),
                List.of("오늘의 핵심 비선호: 면 요리"),
                10
        ));

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(openAiJsonClient).requestJson(
                systemPrompt.capture(),
                userPrompt.capture(),
                anyInt()
        );
        assertThat(systemPrompt.getValue())
                .contains("오늘의 핵심 선호", "오늘의 핵심 비선호", "일반 선호보다 우선")
                .contains("음식권, 맛, 온도, 재료, 식감, 음식 형태, 식사 상황")
                .contains("무관한 메뉴를 섞지 않는다");
        assertThat(userPrompt.getValue())
                .contains("오늘의 핵심 선호: 매콤한, 국물")
                .contains("오늘의 핵심 비선호: 면 요리");
    }
}
