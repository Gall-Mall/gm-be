package com.gm.api.controller.vote.candidate;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmission;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.candidate.service.MenuVoteFinalizationService;
import com.gm.core.domain.vote.candidate.service.MenuVoteService;
import com.gm.core.domain.vote.candidate.service.FinalMenuSelectionService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuCandidateControllerTest {

    @Test
    @DisplayName("투표 세션의 메뉴 후보를 노출 순서대로 반환한다")
    void findMenuCandidates_returnsCandidateList() throws Exception {
        MenuCandidateService menuCandidateService = mock(MenuCandidateService.class);
        MenuCandidateController controller = new MenuCandidateController(
                menuCandidateService,
                mock(MenuVoteService.class),
                mock(MenuVoteFinalizationService.class),
                mock(FinalMenuSelectionService.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        MenuVoteCandidate candidate = new MenuVoteCandidate(
                candidateId,
                voteSessionId,
                menuId,
                UUID.randomUUID(),
                "김치찌개",
                "한식",
                "https://example.com/kimchi.jpg",
                1,
                2,
                1,
                0,
                3,
                VoteCandidateResult.PENDING,
                "국물 메뉴 선호 반영"
        );
        given(menuCandidateService.findMenuCandidates(groupId, userId, voteSessionId))
                .willReturn(List.of(candidate));

        User user = new User(
                "테스터", "테스터닉네임", UserStatus.ACTIVE, Provider.NAVER,
                "provider-id", "010-1234-5678", "user@example.com",
                false, null, null, null
        );
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            mockMvc.perform(get(
                            "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates",
                            groupId,
                            voteSessionId
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].voteCandidateId").value(candidateId.toString()))
                    .andExpect(jsonPath("$.data[0].menuId").value(menuId.toString()))
                    .andExpect(jsonPath("$.data[0].menuName").value("김치찌개"))
                    .andExpect(jsonPath("$.data[0].categoryName").value("한식"))
                    .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                    .andExpect(jsonPath("$.data[0].counts.go").value(2))
                    .andExpect(jsonPath("$.data[0].counts.maybe").value(1))
                    .andExpect(jsonPath("$.data[0].counts.no").value(0))
                    .andExpect(jsonPath("$.data[0].respondentCount").value(3))
                    .andExpect(jsonPath("$.data[0].resultStatus").value("PENDING"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("인증 사용자만 메뉴 후보에 투표하고 API 응답 DTO로 최신 집계를 반환한다")
    void submitMenuVote_usesAuthenticatedPrincipalAndMapsResponse() throws Exception {
        MenuCandidateService menuCandidateService = mock(MenuCandidateService.class);
        MenuVoteService menuVoteService = mock(MenuVoteService.class);
        MenuCandidateController controller = new MenuCandidateController(
                menuCandidateService,
                menuVoteService,
                mock(MenuVoteFinalizationService.class),
                mock(FinalMenuSelectionService.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(menuVoteService.submitVote(
                groupId,
                voteSessionId,
                candidateId,
                userId,
                MenuVoteChoice.GO
        )).willReturn(new MenuVoteSubmission(
                MenuVoteChoice.GO,
                new MenuVoteCount(candidateId, 2, 1, 0, 3),
                true
        ));

        User user = new User(
                "테스터", "테스터닉네임", UserStatus.ACTIVE, Provider.NAVER,
                "provider-id", "010-1234-5678", "user@example.com",
                false, null, null, null
        );
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        try {
            mockMvc.perform(put(
                            "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/{candidateId}/vote",
                            groupId,
                            voteSessionId,
                            candidateId
                    )
                    .contentType("application/json")
                    .content("{\"choice\":\"GO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.candidateId").value(candidateId.toString()))
                    .andExpect(jsonPath("$.data.choice").value("GO"))
                    .andExpect(jsonPath("$.data.counts.go").value(2))
                    .andExpect(jsonPath("$.data.counts.maybe").value(1))
                    .andExpect(jsonPath("$.data.counts.no").value(0))
                    .andExpect(jsonPath("$.data.respondentCount").value(3))
                    .andExpect(jsonPath("$.data.changed").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("인증된 방장의 메뉴 투표 조기 마감 결과를 반환한다")
    void finalizeMenuVote_mapsStoredResults() throws Exception {
        MenuVoteFinalizationService finalizationService = mock(MenuVoteFinalizationService.class);
        MenuCandidateController controller = new MenuCandidateController(
                mock(MenuCandidateService.class),
                mock(MenuVoteService.class),
                finalizationService,
                mock(FinalMenuSelectionService.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(finalizationService.finalizeVoteManually(groupId, userId, voteSessionId))
                .willReturn(List.of(new MenuVoteResult(
                        new MenuVoteCount(candidateId, 2, 1, 0, 3),
                        VoteCandidateResult.CONFIRMED
                )));

        User user = new User(
                "테스터", "테스터닉네임", UserStatus.ACTIVE, Provider.NAVER,
                "provider-id", "010-1234-5678", "user@example.com",
                false, null, null, null
        );
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        try {
            mockMvc.perform(put(
                            "/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/close",
                            groupId,
                            voteSessionId
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].candidateId").value(candidateId.toString()))
                    .andExpect(jsonPath("$.data[0].goCount").value(2))
                    .andExpect(jsonPath("$.data[0].result").value("CONFIRMED"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
