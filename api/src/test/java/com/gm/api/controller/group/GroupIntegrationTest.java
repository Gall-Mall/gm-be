package com.gm.api.controller.group;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 그룹 생성(GROUP-001), 내 그룹 목록 조회(GROUP-002), 그룹 상세 조회(GROUP-003),
 * 그룹 정보 수정(GROUP-004) API의 통합 테스트이다.
 *
 * <p>실제 웹 계층부터 H2 기반 영속 계층까지 전체 스택을 거쳐,
 * 요청이 계약대로 저장되고 응답되는지 검증한다. 실제 JWT 인증을 요구하므로
 * {@code spring-security-test}의 {@code authentication(...)} RequestPostProcessor로
 * {@link CustomUserPrincipal}을 직접 SecurityContext에 주입한다(Invite 통합 테스트와 동일한 패턴).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoteSessionService voteSessionService;

    @Autowired
    private GroupService groupService;

    /** 요청 회원을 SecurityContext에 직접 주입하는 RequestPostProcessor를 만든다. */
    private static RequestPostProcessor authAs(UUID userId) {
        User dummyUser = new User(
                "테스터", "테스터닉네임",
                UserStatus.ACTIVE, Provider.NAVER, "naver-provider-id",
                "010-1234-5678", "user@example.com",
                false, null,
                null, null
        );

        CustomUserPrincipal principal = new CustomUserPrincipal(userId, dummyUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(authentication);
    }

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
                        .with(authAs(ownerUserId))
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
                        .with(authAs(UUID.randomUUID()))
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
                        .with(authAs(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("인증 없이 요청하면 COMMON-003 오류를 반환한다")
    void createGroup_withoutAuthentication_returnsCommon003() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("내가 참여 중인 그룹만 목록으로 조회한다")
    void findMyGroups_returnsGroupsOfActiveMember() throws Exception {
        UUID ownerUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/groups")
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/groups")
                        .with(authAs(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].ownerUserId").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.data.content[0].name").value("점심팟"))
                .andExpect(jsonPath("$.data.content[0].memberCount").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("참여 중인 그룹이 없으면 빈 목록을 반환한다")
    void findMyGroups_withNoMemberships_returnsEmptyContent() throws Exception {
        mockMvc.perform(get("/api/groups")
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("투표 세션이 없으면 생성일 내림차순으로 정렬되고, size만큼 페이지가 나뉜다")
    void findMyGroups_ordersByCreatedAtDesc_andPaginates() throws Exception {
        UUID ownerUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/groups")
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated());

        String secondGroupBody = """
                {
                  "name": "저녁팟",
                  "locationAddress": "서울특별시 강남구 테헤란로 456",
                  "latitude": 37.5012345,
                  "longitude": 127.0398765,
                  "searchRadiusM": 1000,
                  "recommendationTime": "18:00",
                  "maxMemberCount": 6
                }
                """;
        mockMvc.perform(post("/api/groups")
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondGroupBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/groups")
                        .with(authAs(ownerUserId))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("저녁팟"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/api/groups")
                        .with(authAs(ownerUserId))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("점심팟"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("인증 없이 요청하면 COMMON-003 오류를 반환한다")
    void findMyGroups_withoutAuthentication_returnsCommon003() throws Exception {
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("최근 투표 세션이 생성된 그룹이, 더 먼저 만들어진 그룹보다 앞에 온다")
    void findMyGroups_ordersByMostRecentVoteSessionActivity() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID testUserId = ownerUserId;

        UUID firstGroupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        String secondGroupBody = """
                {
                  "name": "저녁팟",
                  "locationAddress": "서울특별시 강남구 테헤란로 456",
                  "latitude": 37.5012345,
                  "longitude": 127.0398765,
                  "searchRadiusM": 1000,
                  "recommendationTime": "18:00",
                  "maxMemberCount": 6
                }
                """;
        UUID secondGroupId = createGroupAndGetId(ownerUserId, secondGroupBody);

        // 생성 순서상으로는 저녁팟(둘째 그룹)이 앞서야 하지만,
        // 점심팟(첫째 그룹)에 더 나중에 투표 세션이 생겨 활동이 더 최근이다.
        voteSessionService.createManualVoteSession(secondGroupId, testUserId, "저녁 투표", null, null);
        voteSessionService.createManualVoteSession(firstGroupId, testUserId, "점심 투표", null, null);

        mockMvc.perform(get("/api/groups")
                        .with(authAs(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("점심팟"))
                .andExpect(jsonPath("$.data.content[1].name").value("저녁팟"));
    }

    @Test
    @DisplayName("그룹장이 상세 조회하면 200과 함께 그룹 정보·OWNER 역할을 반환한다")
    void findGroupDetail_succeeds_forOwner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .with(authAs(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.data.ownerUserId").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.data.name").value("점심팟"))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.currentUserRole").value("OWNER"));
    }

    @Test
    @DisplayName("존재하지 않는 groupId를 조회하면 GROUP-001 오류를 반환한다")
    void findGroupDetail_withUnknownGroupId_returnsGroup001() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", UUID.randomUUID())
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP-001"));
    }

    @Test
    @DisplayName("그룹의 활성 멤버가 아닌 회원이 조회하면 GROUP-002 오류를 반환한다")
    void findGroupDetail_withNonMemberRequester_returnsGroup002() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        // 그룹장과 다른 비회원 UUID 생성
        UUID nonMemberUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .with(authAs(nonMemberUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP-002"));
    }

    @Test
    @DisplayName("groupId가 UUID 형식이 아니면 COMMON-005 오류를 반환한다")
    void findGroupDetail_withMalformedGroupId_returnsCommon005() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", "not-a-uuid")
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-005"));
    }

    @Test
    @DisplayName("인증 없이 요청하면 COMMON-003 오류를 반환한다")
    void findGroupDetail_withoutAuthentication_returnsCommon003() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    private static final String UPDATE_REQUEST_BODY = """
            {
              "name": "저녁팟",
              "locationAddress": "서울특별시 강남구 역삼동",
              "latitude": 37.5001,
              "longitude": 127.0365,
              "searchRadiusM": 2000,
              "recommendationTime": "17:30",
              "maxMemberCount": 6
            }
            """;

    @Test
    @DisplayName("그룹장이 수정하면 200과 함께 교체된 그룹 정보를 반환한다")
    void updateGroup_succeeds_forOwner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.data.ownerUserId").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.data.name").value("저녁팟"))
                .andExpect(jsonPath("$.data.locationAddress").value("서울특별시 강남구 역삼동"))
                .andExpect(jsonPath("$.data.searchRadiusM").value(2000))
                .andExpect(jsonPath("$.data.recommendationTime").value("17:30"))
                .andExpect(jsonPath("$.data.maxMemberCount").value(6))
                .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    @DisplayName("수정에 성공하면 updatedAt이 갱신된 값으로 응답된다")
    void updateGroup_succeeds_refreshesUpdatedAt() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/groups")
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        String groupId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.groupId");
        String createdUpdatedAt = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.updatedAt");

        MvcResult updateResult = mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isOk())
                .andReturn();
        String updatedUpdatedAt = JsonPath.read(updateResult.getResponse().getContentAsString(), "$.data.updatedAt");

        assertThat(updatedUpdatedAt).isNotEqualTo(createdUpdatedAt);
    }

    @Test
    @DisplayName("정원을 현재 활성 멤버 수보다 작게 수정하면 GROUP-008 오류를 반환한다")
    void updateGroup_withCapacityBelowActiveMembers_returnsGroup008() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);
        groupService.addMember(groupId, UUID.randomUUID());

        String body = """
                {
                  "name": "저녁팟",
                  "locationAddress": "서울특별시 강남구 역삼동",
                  "latitude": 37.5001,
                  "longitude": 127.0365,
                  "searchRadiusM": 2000,
                  "recommendationTime": "17:30",
                  "maxMemberCount": 1
                }
                """;

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP-008"));
    }

    @Test
    @DisplayName("존재하지 않는 groupId를 수정하면 GROUP-001 오류를 반환한다")
    void updateGroup_withUnknownGroupId_returnsGroup001() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}", UUID.randomUUID())
                        .with(authAs(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP-001"));
    }

    @Test
    @DisplayName("그룹장이 아닌 회원이 수정하면 GROUP-006 오류를 반환한다")
    void updateGroup_withNonOwnerRequester_returnsGroup006() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .with(authAs(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP-006"));
    }

    @Test
    @DisplayName("그룹명이 비어 있으면 COMMON-002 오류를 반환한다")
    void updateGroup_withBlankName_returnsCommon002() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);

        String body = """
                {
                  "name": "",
                  "locationAddress": "서울특별시 강남구 역삼동",
                  "latitude": 37.5001,
                  "longitude": 127.0365,
                  "searchRadiusM": 2000,
                  "recommendationTime": "17:30",
                  "maxMemberCount": 6
                }
                """;

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("groupId가 UUID 형식이 아니면 COMMON-005 오류를 반환한다")
    void updateGroup_withMalformedGroupId_returnsCommon005() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}", "not-a-uuid")
                        .with(authAs(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-005"));
    }

    @Test
    @DisplayName("인증 없이 요청하면 COMMON-003 오류를 반환한다")
    void updateGroup_withoutAuthentication_returnsCommon003() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("활성 그룹 멤버는 방장을 포함한 멤버 목록을 조회할 수 있다")
    void findMembers_asMember_returnsActiveMembers() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);
        groupService.addMember(groupId, memberUserId);

        mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                        .with(authAs(memberUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.userId == '" + ownerUserId + "')].role").value("OWNER"))
                .andExpect(jsonPath("$.data[?(@.userId == '" + memberUserId + "')].role").value("MEMBER"));
    }

    @Test
    @DisplayName("그룹장은 활성 멤버에게 방장을 위임할 수 있다")
    void transferOwner_asOwner_changesMemberRoles() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);
        groupService.addMember(groupId, memberUserId);

        mockMvc.perform(patch("/api/groups/{groupId}/owner", groupId)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberUserId + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                        .with(authAs(memberUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.userId == '" + ownerUserId + "')].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[?(@.userId == '" + memberUserId + "')].role").value("OWNER"));
    }

    @Test
    @DisplayName("그룹장은 일반 멤버를 강퇴할 수 있다")
    void kickMember_asOwner_removesMemberFromActiveList() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = createGroupAndGetId(ownerUserId, REQUEST_BODY);
        groupService.addMember(groupId, memberUserId);

        mockMvc.perform(delete("/api/groups/{groupId}/members/{userId}", groupId, memberUserId)
                        .with(authAs(ownerUserId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                        .with(authAs(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(ownerUserId.toString()));
    }

    private UUID createGroupAndGetId(UUID ownerUserId, String requestBody) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        String groupId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.groupId");
        return UUID.fromString(groupId);
    }
}
