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
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuCandidateControllerTest {

    @Test
    @DisplayName("투표 세션의 메뉴 후보를 노출 순서대로 반환한다")
    void findMenuCandidates_returnsCandidateList() throws Exception {
        MenuCandidateService menuCandidateService = mock(MenuCandidateService.class);
        MenuCandidateController controller = new MenuCandidateController(menuCandidateService);
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
}
