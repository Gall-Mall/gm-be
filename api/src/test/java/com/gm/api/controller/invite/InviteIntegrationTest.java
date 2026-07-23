package com.gm.api.controller.invite;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.invite.service.InviteService;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 코드 생성(INVITE-001), 초대 정보 조회(INVITE-002), 초대 코드로 그룹 가입(INVITE-003),
 * rate limiting의 통합 테스트이다.
 *
 * <p>Group 컨트롤러와 달리 이 세 엔드포인트는 실제 JWT 인증을 요구하므로,
 * {@code X-User-Id} 헤더 대신 {@code spring-security-test}의 {@code authentication(...)}
 * RequestPostProcessor로 {@link CustomUserPrincipal}을 직접 SecurityContext에 주입한다.
 * {@code JwtAuthenticationFilter}는 {@code Authorization} 헤더가 없으면 기존 인증 정보를
 * 건드리지 않고 통과시키므로 이 방식이 정상 동작한다. Invite 로직 자체가 {@code UserService}를
 * 호출하지 않으므로, 주입하는 사용자가 실제 DB에 저장돼 있을 필요는 없다.</p>
 *
 * <p>이 테스트는 {@code localhost:16379}에 실제 Redis가 떠 있어야 한다
 * ({@code docker-compose up -d}) — 초대 코드 저장과 rate limiting 카운터 모두 Redis를
 * 사용하며, MySQL과 달리 테스트용 임베디드 대체재가 없다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class InviteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    @Autowired
    private InviteService inviteService;

    /** 요청 회원을 SecurityContext에 직접 주입하는 RequestPostProcessor를 만든다. */
    private static RequestPostProcessor authAs(UUID userId) {
        User dummyUser = User.create("테스터", UserStatus.ACTIVE, Provider.NAVER, userId.toString(), "010-0000-0000", "test@example.com", false, null, null, null);
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, dummyUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(authentication);
    }

    /** 요청 회원을 그룹장으로 하는 그룹을 생성한다. */
    private Group createGroup(UUID ownerUserId, int maxMemberCount) {
        NewGroup newGroup = new NewGroup(
                ownerUserId, "점심팟", "서울특별시 강남구 테헤란로 123",
                37.5012345, 127.0398765, 1000, LocalTime.of(11, 0), maxMemberCount
        );
        return groupService.create(newGroup);
    }

    @Test
    @DisplayName("그룹장이 초대 코드를 요청하면 201과 함께 6자리 코드와 초대 링크를 반환한다")
    void createInvite_succeeds_whenRequesterIsOwner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);

        MvcResult result = mockMvc.perform(post("/api/groups/{groupId}/invites", group.id())
                        .with(authAs(ownerUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String inviteCode = JsonPath.read(responseBody, "$.data.inviteCode");
        String inviteUrl = JsonPath.read(responseBody, "$.data.inviteUrl");

        assertThat(inviteCode).matches("[0-9a-zA-Z]{6}");
        assertThat(inviteUrl).isEqualTo("http://localhost:5173/invites/" + inviteCode);
    }

    @Test
    @DisplayName("그룹장이 아닌 회원이 초대 코드를 요청하면 GROUP-006 오류를 반환한다")
    void createInvite_returnsGroup006_whenRequesterIsNotOwner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);

        mockMvc.perform(post("/api/groups/{groupId}/invites", group.id())
                        .with(authAs(otherUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GROUP-006"));
    }

    @Test
    @DisplayName("존재하지 않는 그룹에 초대 코드를 요청하면 GROUP-001 오류를 반환한다")
    void createInvite_returnsGroup001_whenGroupDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/invites", UUID.randomUUID())
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP-001"));
    }

    @Test
    @DisplayName("정원이 가득 찬 그룹의 그룹장이 초대 코드를 요청하면 GROUP-004 오류를 반환한다")
    void createInvite_returnsGroup004_whenGroupIsFull() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        // 정원 1명 그룹은 생성 시점에 그룹장 혼자로 이미 가득 찬다.
        Group group = createGroup(ownerUserId, 1);

        mockMvc.perform(post("/api/groups/{groupId}/invites", group.id())
                        .with(authAs(ownerUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROUP-004"));
    }

    @Test
    @DisplayName("인증 없이 초대 코드를 요청하면 COMMON-003 오류를 반환한다")
    void createInvite_returnsCommon003_whenUnauthenticated() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);

        mockMvc.perform(post("/api/groups/{groupId}/invites", group.id()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("정원이 남은 그룹의 초대 코드를 조회하면 joinable=true를 반환한다")
    void getInviteInfo_returnsJoinableTrue_whenGroupHasCapacity() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        mockMvc.perform(get("/api/invites/{inviteCode}", inviteCode)
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").value(inviteCode))
                .andExpect(jsonPath("$.data.groupId").value(group.id().toString()))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.maxMemberCount").value(6))
                .andExpect(jsonPath("$.data.joinable").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드를 조회하면 INVITE-001 오류를 반환한다")
    void getInviteInfo_returnsInvite001_whenCodeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/invites/{inviteCode}", "ZZ99ZZ")
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITE-001"));
    }

    @Test
    @DisplayName("초대 코드로 가입에 성공하면 201과 함께 MEMBER 역할·ACTIVE 상태의 멤버십을 반환한다")
    void joinByInviteCode_succeeds() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID joinerUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode)
                        .with(authAs(joinerUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupId").value(group.id().toString()))
                .andExpect(jsonPath("$.data.userId").value(joinerUserId.toString()))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드로 가입을 시도하면 INVITE-001 오류를 반환한다")
    void joinByInviteCode_returnsInvite001_whenCodeDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/invites/{inviteCode}/members", "ZZ99ZZ")
                        .with(authAs(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITE-001"));
    }

    @Test
    @DisplayName("이미 가입한 그룹에 같은 초대 코드로 재가입을 시도하면 INVITE-002 오류를 반환한다")
    void joinByInviteCode_returnsInvite002_whenAlreadyMember() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID joinerUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(joinerUserId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(joinerUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITE-002"));
    }

    @Test
    @DisplayName("가입 도중 그룹이 가득 차면 GROUP-004 오류를 반환한다")
    void joinByInviteCode_returnsGroup004_whenGroupFillsUpAfterCodeIssued() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID firstJoinerUserId = UUID.randomUUID();
        UUID secondJoinerUserId = UUID.randomUUID();
        // 정원 2명: 그룹장(1) + 첫 번째 가입자(1) = 정원 도달. 코드 발급 시점엔 자리가 있었다.
        Group group = createGroup(ownerUserId, 2);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(firstJoinerUserId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(secondJoinerUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROUP-004"));
    }

    @Test
    @DisplayName("인증 없이 초대 코드로 가입을 시도하면 COMMON-003 오류를 반환한다")
    void joinByInviteCode_returnsCommon003_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/invites/{inviteCode}/members", "ZZ99ZZ"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("정원이 하나 남은 그룹에 두 사용자가 동시에 가입을 시도하면 한 명만 성공한다")
    void joinByInviteCode_onlyOneSucceeds_whenTwoUsersRaceForLastSpot() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        // 정원 2명: 그룹장(1) + 마지막 한 자리를 두 사용자가 동시에 다툰다.
        Group group = createGroup(ownerUserId, 2);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        // 두 스레드가 동시에 요청을 시작하도록 CountDownLatch로 시작 시점을 맞춘다.
        // PESSIMISTIC_WRITE 락이 없다면 두 트랜잭션 모두 "정원 미달"로 읽어 둘 다 성공(정원 초과)할 수 있다.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> firstAttempt =
                    executor.submit(joinAttempt(inviteCode, firstUserId, ready, start));
            Future<Integer> secondAttempt =
                    executor.submit(joinAttempt(inviteCode, secondUserId, ready, start));

            ready.await();
            start.countDown();

            int firstStatus = firstAttempt.get();
            int secondStatus = secondAttempt.get();

            assertThat(List.of(firstStatus, secondStatus)).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdown();
        }
    }

    /** 두 스레드가 {@link CountDownLatch}로 시작 시점을 맞춰 같은 초대 코드로 가입을 시도하는 작업을 만든다. */
    private Callable<Integer> joinAttempt(String inviteCode, UUID requesterId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            MvcResult result = mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode)
                            .with(authAs(requesterId)))
                    .andReturn();
            return result.getResponse().getStatus();
        };
    }

    @Test
    @DisplayName("같은 사용자가 그룹 가입을 분당 한도보다 많이 호출하면 COMMON-006 오류를 반환한다")
    void joinByInviteCode_returnsCommon006_whenExceedingPerMinuteRateLimit() throws Exception {
        UUID requesterUserId = UUID.randomUUID();

        // application.yml의 app.rate-limit.invite.join-per-minute 기본값(10)만큼은 통과해야 한다.
        // 시간당 한도(30)보다 낮아 분당 한도가 먼저 걸리는지 검증한다. 매번 다른 그룹으로 가입해야
        // "이미 가입함"(INVITE-002)이 아니라 순수하게 rate limit에 걸리는지 확인할 수 있다.
        for (int i = 0; i < 10; i++) {
            UUID ownerUserId = UUID.randomUUID();
            Group group = createGroup(ownerUserId, 6);
            String inviteCode = inviteService.create(group.id(), ownerUserId);

            mockMvc.perform(post("/api/invites/{inviteCode}/members", inviteCode).with(authAs(requesterUserId)))
                    .andExpect(status().isCreated());
        }

        UUID extraOwnerUserId = UUID.randomUUID();
        Group extraGroup = createGroup(extraOwnerUserId, 6);
        String extraInviteCode = inviteService.create(extraGroup.id(), extraOwnerUserId);

        mockMvc.perform(post("/api/invites/{inviteCode}/members", extraInviteCode).with(authAs(requesterUserId)))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value("COMMON-006"));
    }

    @Test
    @DisplayName("같은 사용자가 초대 정보 조회를 분당 한도보다 많이 호출하면 COMMON-006 오류를 반환한다")
    void getInviteInfo_returnsCommon006_whenExceedingRateLimit() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        Group group = createGroup(ownerUserId, 6);
        String inviteCode = inviteService.create(group.id(), ownerUserId);

        // application.yml의 app.rate-limit.invite.info-per-minute 기본값(20)만큼은 통과해야 한다.
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/invites/{inviteCode}", inviteCode).with(authAs(requesterUserId)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/invites/{inviteCode}", inviteCode).with(authAs(requesterUserId)))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value("COMMON-006"));
    }
}
