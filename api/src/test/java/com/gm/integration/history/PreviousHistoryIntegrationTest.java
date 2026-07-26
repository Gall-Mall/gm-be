package com.gm.integration.history;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.gm.api.ApiApplication;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.store.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.db.domain.group.entity.DiningGroupEntity;
import com.gm.db.domain.group.entity.GroupMemberEntity;
import com.gm.db.domain.group.repository.GroupJpaRepository;
import com.gm.db.domain.group.repository.GroupMemberJpaRepository;
import com.gm.db.domain.store.entity.StoreEntity;
import com.gm.db.domain.store.repository.StoreJpaRepository;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import com.gm.db.domain.vote.candidate.repository.VoteCandidateJpaRepository;
import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import com.gm.db.domain.vote.session.repository.VoteSessionJpaRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:previous-history;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class PreviousHistoryIntegrationTest {

    private static final String URI = "/api/users/me/previous-groups";
    private static final String DETAIL_URI =
            "/api/users/me/previous-vote-sessions/{voteSessionId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupJpaRepository groupJpaRepository;

    @Autowired
    private GroupMemberJpaRepository groupMemberJpaRepository;

    @Autowired
    private VoteSessionJpaRepository voteSessionJpaRepository;

    @Autowired
    private StoreJpaRepository storeJpaRepository;

    @Autowired
    private VoteCandidateJpaRepository voteCandidateJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("그룹별 완료 세션의 선택 식당을 세션 완료 시각 최신순으로 반환한다")
    void getPreviousHistory_returnsSelectedRestaurantsGroupedByRecentClosedAt() throws Exception {
        UUID userId = UUID.randomUUID();
        DiningGroupEntity lunchGroup = createGroup(userId, "강남 점심 모임");
        DiningGroupEntity dinnerGroup = createGroup(userId, "판교 저녁 모임");

        // 식당 생성 시각과 세션 완료 시각을 반대로 만들어 closedAt 정렬임을 검증한다.
        VoteSessionEntity lunchNewestSession = createSession(
                lunchGroup.getId(), VoteSessionStatus.COMPLETED, "점심 최신 세션");
        VoteSessionEntity dinnerSession = createSession(
                dinnerGroup.getId(), VoteSessionStatus.COMPLETED, "저녁 세션");
        VoteSessionEntity lunchOlderSession = createSession(
                lunchGroup.getId(), VoteSessionStatus.COMPLETED, "점심 이전 세션");

        StoreEntity lunchNewest = createRestaurant(
                lunchNewestSession.getId(), true, "이자카야 하루", "lunch-newest", 120);
        StoreEntity dinner = createRestaurant(
                dinnerSession.getId(), true, "파스타 브라더스", "dinner", 220);
        StoreEntity lunchOlder = createRestaurant(
                lunchOlderSession.getId(), true, "앤티크 커피", "lunch-older", 320);
        createCandidate(lunchNewestSession.getId(), true, 3, 1, 0);
        createCandidate(dinnerSession.getId(), true, 2, 1, 1);
        createCandidate(lunchOlderSession.getId(), true, 1, 0, 2);
        setCreatedAt(lunchNewest.getId(), LocalDateTime.of(2026, 7, 23, 12, 0));
        setCreatedAt(dinner.getId(), LocalDateTime.of(2026, 7, 25, 18, 0));
        setCreatedAt(lunchOlder.getId(), LocalDateTime.of(2026, 7, 24, 12, 0));
        setClosedAt(lunchNewestSession.getId(), LocalDateTime.of(2026, 7, 25, 12, 0));
        setClosedAt(dinnerSession.getId(), LocalDateTime.of(2026, 7, 24, 18, 0));
        setClosedAt(lunchOlderSession.getId(), LocalDateTime.of(2026, 7, 23, 12, 0));

        mockMvc.perform(get(URI).with(authAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.previous.length()").value(2))
                .andExpect(jsonPath("$.data.previous[0].groupId")
                        .value(lunchGroup.getId().toString()))
                .andExpect(jsonPath("$.data.previous[0].name").value("강남 점심 모임"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions.length()").value(2))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].voteSessionId")
                        .value(lunchNewestSession.getId().toString()))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].name")
                        .value("이자카야 하루"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].url")
                        .value("https://place.map.kakao.com/lunch-newest"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].address")
                        .value("서울특별시 강남구"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].latitude").value(37.5))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].longitude").value(127.0))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].distanceM").value(120))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].externalPlaceId")
                        .value("lunch-newest"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].completedAt")
                        .value("2026-07-25T12:00:00"))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].goCount").value(3))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].maybeCount").value(1))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].noCount").value(0))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[1].voteSessionId")
                        .value(lunchOlderSession.getId().toString()))
                .andExpect(jsonPath("$.data.previous[1].groupId")
                        .value(dinnerGroup.getId().toString()))
                .andExpect(jsonPath("$.data.previous[1].voteSessions[0].voteSessionId")
                        .value(dinnerSession.getId().toString()));
    }

    @Test
    @DisplayName("활성 멤버의 완료 세션에서 선택된 식당만 반환한다")
    void getPreviousHistory_filtersMembershipSessionStatusAndSelectedRestaurant() throws Exception {
        UUID userId = UUID.randomUUID();
        DiningGroupEntity activeGroup = createGroup(userId, "포함 그룹");
        DiningGroupEntity otherUserGroup = createGroup(UUID.randomUUID(), "다른 회원 그룹");
        DiningGroupEntity leftGroup = createGroup(userId, "탈퇴 그룹");
        setMemberStatus(leftGroup.getId(), userId, GroupMemberStatus.LEFT);

        VoteSessionEntity includedSession = createSession(
                activeGroup.getId(), VoteSessionStatus.COMPLETED, "포함 세션");
        createRestaurant(includedSession.getId(), true, "포함 식당", "included", 100);
        createCandidate(includedSession.getId(), true, 3, 0, 0);

        VoteSessionEntity incompleteSession = createSession(
                activeGroup.getId(), VoteSessionStatus.RESTAURANT_SELECTION, "진행 중 세션");
        createRestaurant(incompleteSession.getId(), true, "진행 중 식당", "incomplete", 200);
        createCandidate(incompleteSession.getId(), true, 2, 1, 0);

        VoteSessionEntity unselectedSession = createSession(
                activeGroup.getId(), VoteSessionStatus.COMPLETED, "미선택 세션");
        createRestaurant(unselectedSession.getId(), false, "미선택 식당", "unselected", 300);
        createCandidate(unselectedSession.getId(), true, 1, 1, 1);

        VoteSessionEntity unselectedCandidateSession = createSession(
                activeGroup.getId(), VoteSessionStatus.COMPLETED, "메뉴 미선택 세션");
        createRestaurant(
                unselectedCandidateSession.getId(),
                true,
                "메뉴 미선택 식당",
                "unselected-candidate",
                350
        );
        createCandidate(unselectedCandidateSession.getId(), false, 1, 1, 1);

        VoteSessionEntity otherUserSession = createSession(
                otherUserGroup.getId(), VoteSessionStatus.COMPLETED, "다른 회원 세션");
        createRestaurant(otherUserSession.getId(), true, "다른 회원 식당", "other-user", 400);
        createCandidate(otherUserSession.getId(), true, 3, 0, 0);

        VoteSessionEntity leftGroupSession = createSession(
                leftGroup.getId(), VoteSessionStatus.COMPLETED, "탈퇴 그룹 세션");
        createRestaurant(leftGroupSession.getId(), true, "탈퇴 그룹 식당", "left", 500);
        createCandidate(leftGroupSession.getId(), true, 3, 0, 0);

        mockMvc.perform(get(URI).with(authAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previous.length()").value(1))
                .andExpect(jsonPath("$.data.previous[0].groupId")
                        .value(activeGroup.getId().toString()))
                .andExpect(jsonPath("$.data.previous[0].voteSessions.length()").value(1))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].voteSessionId")
                        .value(includedSession.getId().toString()))
                .andExpect(jsonPath("$.data.previous[0].voteSessions[0].name")
                        .value("포함 식당"));
    }

    @Test
    @DisplayName("지난 기록이 없으면 빈 배열을 반환한다")
    void getPreviousHistory_returnsEmptyArrayWhenNoHistoryExists() throws Exception {
        mockMvc.perform(get(URI).with(authAs(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previous").isArray())
                .andExpect(jsonPath("$.data.previous.length()").value(0));
    }

    @Test
    @DisplayName("인증 없이 지난 기록을 요청하면 401을 반환한다")
    void getPreviousHistory_returnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get(URI))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    @Test
    @DisplayName("완료된 지난 기록을 투표 세션 식별자로 상세 조회한다")
    void getPreviousHistoryDetail_returnsSelectedRestaurantAndMenuVoteCounts() throws Exception {
        UUID userId = UUID.randomUUID();
        DiningGroupEntity group = createGroup(userId, "강남 점심 모임");
        VoteSessionEntity session = createSession(
                group.getId(),
                VoteSessionStatus.COMPLETED,
                "점심 세션"
        );
        createRestaurant(
                session.getId(),
                true,
                "이자카야 하루",
                "detail-place",
                120
        );
        createCandidate(session.getId(), true, 3, 1, 0);
        setClosedAt(session.getId(), LocalDateTime.of(2026, 7, 25, 12, 0));

        mockMvc.perform(get(DETAIL_URI, session.getId()).with(authAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupId").value(group.getId().toString()))
                .andExpect(jsonPath("$.data.groupName").value("강남 점심 모임"))
                .andExpect(jsonPath("$.data.voteSessionId")
                        .value(session.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("이자카야 하루"))
                .andExpect(jsonPath("$.data.url")
                        .value("https://place.map.kakao.com/detail-place"))
                .andExpect(jsonPath("$.data.address").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.data.latitude").value(37.5))
                .andExpect(jsonPath("$.data.longitude").value(127.0))
                .andExpect(jsonPath("$.data.distanceM").value(120))
                .andExpect(jsonPath("$.data.externalPlaceId").value("detail-place"))
                .andExpect(jsonPath("$.data.goCount").value(3))
                .andExpect(jsonPath("$.data.maybeCount").value(1))
                .andExpect(jsonPath("$.data.noCount").value(0))
                .andExpect(jsonPath("$.data.completedAt").value("2026-07-25T12:00:00"));
    }

    @Test
    @DisplayName("다른 사용자의 지난 기록은 상세 조회할 수 없다")
    void getPreviousHistoryDetail_withOtherUsersSession_returnsSessionNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        DiningGroupEntity group = createGroup(ownerId, "다른 사용자 그룹");
        VoteSessionEntity session = createSession(
                group.getId(),
                VoteSessionStatus.COMPLETED,
                "다른 사용자 세션"
        );
        createRestaurant(session.getId(), true, "다른 사용자 식당", "other-detail", 200);
        createCandidate(session.getId(), true, 2, 1, 0);

        mockMvc.perform(get(DETAIL_URI, session.getId()).with(authAs(requesterId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SESSION-001"));
    }

    @Test
    @DisplayName("완료되지 않은 세션은 지난 기록으로 상세 조회할 수 없다")
    void getPreviousHistoryDetail_withIncompleteSession_returnsSessionNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        DiningGroupEntity group = createGroup(userId, "진행 중 그룹");
        VoteSessionEntity session = createSession(
                group.getId(),
                VoteSessionStatus.RESTAURANT_SELECTION,
                "진행 중 세션"
        );
        createRestaurant(session.getId(), true, "진행 중 식당", "incomplete-detail", 200);
        createCandidate(session.getId(), true, 2, 1, 0);

        mockMvc.perform(get(DETAIL_URI, session.getId()).with(authAs(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SESSION-001"));
    }

    @Test
    @DisplayName("존재하지 않는 지난 기록 상세 조회는 SESSION-001을 반환한다")
    void getPreviousHistoryDetail_withMissingSession_returnsSessionNotFound() throws Exception {
        mockMvc.perform(get(DETAIL_URI, UUID.randomUUID()).with(authAs(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SESSION-001"));
    }

    @Test
    @DisplayName("인증 없이 지난 기록 상세를 요청하면 401을 반환한다")
    void getPreviousHistoryDetail_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(DETAIL_URI, UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    private DiningGroupEntity createGroup(UUID userId, String name) {
        DiningGroupEntity group = groupJpaRepository.saveAndFlush(new DiningGroupEntity(
                userId,
                name,
                "서울특별시 강남구",
                37.5,
                127.0,
                1000,
                LocalTime.NOON,
                10
        ));
        groupMemberJpaRepository.saveAndFlush(GroupMemberEntity.ofOwner(group.getId(), userId));
        return group;
    }

    private VoteSessionEntity createSession(
            UUID groupId,
            VoteSessionStatus status,
            String title
    ) {
        return voteSessionJpaRepository.saveAndFlush(new VoteSessionEntity(
                groupId,
                status,
                title,
                null,
                null,
                null,
                null
        ));
    }

    private StoreEntity createRestaurant(
            UUID voteSessionId,
            boolean selected,
            String name,
            String externalPlaceId,
            int distanceM
    ) {
        return storeJpaRepository.saveAndFlush(new StoreEntity(
                voteSessionId,
                selected,
                name,
                "https://place.map.kakao.com/" + externalPlaceId,
                "서울특별시 강남구",
                37.5,
                127.0,
                distanceM,
                externalPlaceId,
                Provider.KAKAO
        ));
    }

    private VoteCandidateEntity createCandidate(
            UUID voteSessionId,
            boolean selected,
            int goCount,
            int maybeCount,
            int noCount
    ) {
        return voteCandidateJpaRepository.saveAndFlush(new VoteCandidateEntity(
                voteSessionId,
                UUID.randomUUID(),
                1,
                selected,
                goCount,
                maybeCount,
                noCount,
                goCount + maybeCount + noCount,
                VoteCandidateResult.CONFIRMED,
                "추천 이유"
        ));
    }

    private void setCreatedAt(UUID restaurantId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "update recommended_restaurant set created_at = ? where id = ?",
                Timestamp.valueOf(createdAt),
                restaurantId
        );
    }

    private void setClosedAt(UUID voteSessionId, LocalDateTime closedAt) {
        jdbcTemplate.update(
                "update vote_session set closed_at = ? where id = ?",
                Timestamp.valueOf(closedAt),
                voteSessionId
        );
    }

    private void setMemberStatus(
            UUID groupId,
            UUID userId,
            GroupMemberStatus status
    ) {
        jdbcTemplate.update(
                "update group_member set status = ? where dining_group_id = ? and user_id = ?",
                status.name(),
                groupId,
                userId
        );
    }

    private static RequestPostProcessor authAs(UUID userId) {
        User dummyUser = new User(
                "테스터",
                "테스터닉네임",
                UserStatus.ACTIVE,
                com.gm.core.domain.user.model.Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com",
                false,
                null,
                null,
                null
        );

        CustomUserPrincipal principal = new CustomUserPrincipal(userId, dummyUser);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        return authentication(token);
    }
}
