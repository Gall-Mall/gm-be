package com.gm.api.controller.vote.session;

import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.service.VoteSessionService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoteSessionIntegrationTest {

    private static final String VALID_REQUEST_BODY = """
            {
              "title": "저녁 메뉴 투표",
              "likeKeyword": "매콤한 음식",
              "dislikeKeyword": "면 요리"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    @Autowired
    private VoteSessionService voteSessionService;

    private UUID memberUserId;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        memberUserId = createUser();
        groupId = createGroup(memberUserId).id();
    }

    @Test
    @DisplayName("활성 그룹 멤버가 수동 투표 세션을 생성하면 201을 반환한다")
    void createManualVoteSession_returnsVoteSessionId() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .with(authAs(memberUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voteSessionId").exists())
                .andExpect(jsonPath("$.data.status").value("PREFERENCE_INPUT"));
    }

    @Test
    @DisplayName("활성 그룹 멤버가 현재 투표 세션을 조회하면 최신 진행 세션을 반환한다")
    void findCurrentVoteSession_asMember_returnsActiveSession() throws Exception {
        VoteSession session = createVoteSession(groupId, memberUserId);

        mockMvc.perform(get("/api/groups/{groupId}/vote-sessions/current", groupId)
                        .with(authAs(memberUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voteSessionId").value(session.id().toString()))
                .andExpect(jsonPath("$.data.status").value("PREFERENCE_INPUT"));
    }

    @Test
    @DisplayName("진행 중인 투표 세션이 없으면 현재 세션 조회는 null을 반환한다")
    void findCurrentVoteSession_withoutActiveSession_returnsNull() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/vote-sessions/current", groupId)
                        .with(authAs(memberUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 수동 투표 세션을 생성하면 401을 반환한다")
    void createManualVoteSession_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("그룹 멤버가 아니면 수동 투표 세션 생성에 403을 반환한다")
    void createManualVoteSession_asNonMember_returnsForbidden() throws Exception {
        UUID nonMemberUserId = createUser();

        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .with(authAs(nonMemberUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증한 그룹 멤버가 빈 제목을 보내면 400을 반환한다")
    void createManualVoteSession_withBlankTitle_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "title": "",
                  "likeKeyword": null,
                  "dislikeKeyword": null
                }
                """;

        mockMvc.perform(post("/api/groups/{groupId}/vote-sessions", groupId)
                        .with(authAs(memberUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 메뉴 후보를 조회하면 401을 반환한다")
    void findMenuCandidates_withoutAuthentication_returnsUnauthorized() throws Exception {
        VoteSession session = createVoteSession(groupId, memberUserId);

        mockMvc.perform(get(
                        "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates",
                        groupId,
                        session.id()
                ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("그룹 멤버가 아니면 메뉴 후보 조회에 403을 반환한다")
    void findMenuCandidates_asNonMember_returnsForbidden() throws Exception {
        UUID nonMemberUserId = createUser();
        VoteSession session = createVoteSession(groupId, memberUserId);

        mockMvc.perform(get(
                        "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates",
                        groupId,
                        session.id()
                ).with(authAs(nonMemberUserId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("다른 그룹의 세션 ID로 후보를 조회하면 404를 반환한다")
    void findMenuCandidates_withDifferentGroupSession_returnsNotFound() throws Exception {
        UUID otherGroupId = createGroup(memberUserId).id();
        VoteSession session = createVoteSession(groupId, memberUserId);

        mockMvc.perform(get(
                        "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates",
                        otherGroupId,
                        session.id()
                ).with(authAs(memberUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION-001"));
    }

    @Test
    @DisplayName("활성 그룹 멤버는 자신의 그룹 세션 후보를 조회할 수 있다")
    void findMenuCandidates_asMember_returnsCandidates() throws Exception {
        VoteSession session = createVoteSession(groupId, memberUserId);

        mockMvc.perform(get(
                        "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates",
                        groupId,
                        session.id()
                ).with(authAs(memberUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private VoteSession createVoteSession(UUID diningGroupId, UUID userId) {
        return voteSessionService.createManualVoteSession(
                diningGroupId,
                userId,
                "점심 메뉴 투표",
                null,
                null
        );
    }

    private Group createGroup(UUID ownerUserId) {
        return groupService.create(new NewGroup(
                ownerUserId,
                "점심팟-" + UUID.randomUUID(),
                "서울특별시 강남구",
                37.5,
                127.0,
                1000,
                LocalTime.of(11, 30),
                6
        ));
    }

    private UUID createUser() {
        return UUID.randomUUID();
    }

    private static RequestPostProcessor authAs(UUID userId) {
        User user = new User(
                "테스터", "테스터", UserStatus.ACTIVE, Provider.NAVER,
                "provider-id", "010-0000-0000", "user@example.com",
                false, null, null, null
        );
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(token);
    }
}
