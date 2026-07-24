package com.gm.api.controller.invite;

import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.invite.service.InviteService;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 코드로 그룹 가입(INVITE-003)의 시간당 rate limit을 별도 스프링 컨텍스트에서 검증한다.
 *
 * <p>기본 설정(분당 10회 · 시간당 30회)에서는 분당 한도가 항상 먼저 걸려 시간당 한도만
 * 단독으로 관찰할 수 없다. 그래서 이 클래스는 {@link TestPropertySource}로 분당 한도를
 * 크게 늘리고 시간당 한도를 낮춰, 분당 한도에 걸리지 않으면서 시간당 한도만 넘기도록
 * 뒤집는다. {@link InviteIntegrationTest}와 프로퍼티가 달라 별도의 스프링 컨텍스트로
 * 기동되며, 마찬가지로 {@code docker-compose up -d}로 띄운 실제 Redis가 필요하다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.invite.join-per-minute=100",
        "app.rate-limit.invite.join-per-hour=3"
})
class InviteJoinHourlyRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    @Autowired
    private InviteService inviteService;

    private static RequestPostProcessor authAs(UUID userId) {
        User dummyUser = User.create("테스터", Provider.NAVER, userId.toString(), "010-0000-0000", "test@example.com");
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, dummyUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(authentication);
    }

    /**
     * 테스트용 회원 식별자를 발급한다. Invite 로직은 UserService를 호출하지 않으므로
     * 실제 DB에 저장된 회원일 필요가 없다.
     */
    private UUID saveTestUser() {
        return UUID.randomUUID();
    }

    private Group createGroup(UUID ownerUserId, int maxMemberCount) {
        NewGroup newGroup = new NewGroup(
                ownerUserId, "점심팟", "서울특별시 강남구 테헤란로 123",
                37.5012345, 127.0398765, 1000, LocalTime.of(11, 0), maxMemberCount
        );
        return groupService.create(newGroup);
    }

    @Test
    @DisplayName("같은 사용자가 그룹 가입을 시간당 한도보다 많이 호출하면 COMMON-006 오류를 반환한다")
    void joinByInviteCode_returnsCommon006_whenExceedingHourlyRateLimit() throws Exception {
        UUID requesterUserId = saveTestUser();

        // join-per-hour=3까지는 통과해야 한다. 매번 다른 그룹으로 가입해야 INVITE-002(이미 가입함)가
        // 아니라 순수하게 rate limit에 걸리는지 확인할 수 있다.
        for (int i = 0; i < 3; i++) {
            UUID ownerUserId = saveTestUser();
            Group group = createGroup(ownerUserId, 6);
            String inviteCode = inviteService.create(group.id(), ownerUserId);

            mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(requesterUserId)))
                    .andExpect(status().isCreated());
        }

        UUID extraOwnerUserId = saveTestUser();
        Group extraGroup = createGroup(extraOwnerUserId, 6);
        String extraInviteCode = inviteService.create(extraGroup.id(), extraOwnerUserId);

        mockMvc.perform(post("/api/invites/{inviteCode}/members", extraInviteCode).with(authAs(requesterUserId)))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value("COMMON-006"));
    }
}
