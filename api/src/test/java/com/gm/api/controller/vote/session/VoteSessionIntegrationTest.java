package com.gm.api.controller.vote.session;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoteSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("수동 투표 세션을 생성하면 201과 voteSessionId를 반환한다")
    void createManualVoteSession_returnsVoteSessionId() throws Exception {
        UUID groupId = UUID.randomUUID();
        String requestBody = """
                {
                  "title": "저녁 메뉴 투표",
                  "likeKeyword": "한식, 국물",
                  "dislikeKeyword": "회, 느끼한 음식"
                }
                """;

        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voteSessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PREFERENCE_INPUT"));
    }

    @Test
    @DisplayName("제목이 비어 있으면 수동 투표 세션을 생성하지 않는다")
    void createManualVoteSession_withBlankTitle_returnsBadRequest() throws Exception {
        UUID groupId = UUID.randomUUID();
        String requestBody = """
                {
                  "title": "",
                  "likeKeyword": "한식",
                  "dislikeKeyword": "회"
                }
                """;

        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }
}
