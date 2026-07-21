package com.gm.api.controller.group;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 그룹 생성(GROUP-001) API의 통합 테스트이다.
 *
 * <p>실제 웹 계층부터 H2 기반 영속 계층까지 전체 스택을 거쳐,
 * 요청이 계약대로 저장되고 응답되는지 검증한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String REQUEST_BODY = """
            {
              "name": "점심팟",
              "locationAddress": "서울특별시 강남구 테헤란로 123",
              "latitude": 37.5012345,
              "longitude": 127.0398765,
              "searchRadiusM": 1000,
              "recommendationTime": "11:00",
              "maxMemberCount": 6
            }
            """;

    @Test
    @DisplayName("그룹 생성에 성공하면 201과 함께 요청자를 그룹장으로 등록한 그룹 정보를 반환한다")
    void createGroup_succeeds() throws Exception {
        UUID ownerUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/groups")
                        .header("X-User-Id", ownerUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.groupId").exists())
                .andExpect(jsonPath("$.data.ownerUserId").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.data.name").value("점심팟"))
                .andExpect(jsonPath("$.data.locationAddress").value("서울특별시 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.data.searchRadiusM").value(1000))
                .andExpect(jsonPath("$.data.recommendationTime").value("11:00"))
                .andExpect(jsonPath("$.data.maxMemberCount").value(6))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("그룹명이 비어 있으면 COMMON-002 오류를 반환한다")
    void createGroup_withBlankName_returnsCommon002() throws Exception {
        String body = """
                {
                  "name": "",
                  "locationAddress": "서울특별시 강남구 테헤란로 123",
                  "latitude": 37.5012345,
                  "longitude": 127.0398765,
                  "searchRadiusM": 1000,
                  "recommendationTime": "11:00",
                  "maxMemberCount": 6
                }
                """;

        mockMvc.perform(post("/api/groups")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("위도 범위를 벗어나면 COMMON-002 오류를 반환한다")
    void createGroup_withInvalidLatitude_returnsCommon002() throws Exception {
        String body = """
                {
                  "name": "점심팟",
                  "locationAddress": "서울특별시 강남구 테헤란로 123",
                  "latitude": 91,
                  "longitude": 127.0398765,
                  "searchRadiusM": 1000,
                  "recommendationTime": "11:00",
                  "maxMemberCount": 6
                }
                """;

        mockMvc.perform(post("/api/groups")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("요청자 식별 헤더가 없으면 COMMON-002 오류를 반환한다")
    void createGroup_withoutOwnerHeader_returnsCommon002() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }
}
