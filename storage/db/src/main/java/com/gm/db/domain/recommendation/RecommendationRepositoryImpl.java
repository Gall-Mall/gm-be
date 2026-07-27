package com.gm.db.domain.recommendation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.recommendation.model.GroupSoftSignals;
import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;
import com.gm.core.domain.recommendation.repository.RecommendationRepository;
import com.gm.core.domain.user.model.UserCategoryPreference;
import com.gm.core.domain.user.model.UserMenuPreference;
import com.gm.db.domain.group.entity.QGroupMemberEntity;
import com.gm.db.domain.menu.allergen.entity.QMenuAllergenEntity;
import com.gm.db.domain.menu.menu.entity.QMenuEntity;
import com.gm.db.domain.user.entity.QUserAllergenEntity;
import com.gm.db.domain.user.entity.QUserEntity;
import com.gm.db.domain.user.entity.QUserCategoryEntity;
import com.gm.db.domain.user.entity.QUserMenuEntity;
import com.gm.db.domain.vote.candidate.entity.QVoteCandidateEntity;
import com.gm.db.domain.vote.session.entity.QVoteSessionEntity;

import lombok.RequiredArgsConstructor;

/**
 * 스코어링에 필요한 데이터를 DB에서 조립해 core 도메인 모델로 반환하는 구현체.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationRepositoryImpl implements RecommendationRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * ACTIVE 멤버들의 자유텍스트 신호를 컬럼별로 모은다. 비어 있는 값은 버린다.
     */
    @Override
    public GroupSoftSignals findSoftSignalsByGroupId(UUID groupId) {
        QGroupMemberEntity groupMember = QGroupMemberEntity.groupMemberEntity;
        QUserEntity user = QUserEntity.userEntity;

        List<Tuple> rows = queryFactory
                .select(user.customAllergenText, user.preferenceText, user.excludeFoodText)
                .from(groupMember)
                .join(user).on(user.id.eq(groupMember.userId))
                .where(groupMember.diningGroupId.eq(groupId),
                        groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .fetch();

        return new GroupSoftSignals(
                textsOf(rows, user.customAllergenText),
                textsOf(rows, user.preferenceText),
                textsOf(rows, user.excludeFoodText)
        );
    }

    private List<String> textsOf(List<Tuple> rows, StringPath column) {
        return rows.stream()
                .map(row -> row.get(column))
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }

    /**
     * 그룹 ACTIVE 멤버별 선호/알레르기를 조립한다. 메뉴는 LIKE/EXCLUDE, 카테고리는 LIKE/DISLIKE로 갈래를 나눈다.
     */
    @Override
    public List<MemberPreference> findMemberPreferencesByGroupId(UUID groupId) {
        QGroupMemberEntity groupMember = QGroupMemberEntity.groupMemberEntity;

        // ACTIVE 멤버 id만 먼저 조회
        List<UUID> userIds = queryFactory
                .select(groupMember.userId)
                .from(groupMember)
                .where(groupMember.diningGroupId.eq(groupId),
                        groupMember.status.eq(GroupMemberStatus.ACTIVE))
                .fetch();

        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Set<UUID>> likedMenus = new HashMap<>();
        Map<UUID, Set<UUID>> excludeMenus = new HashMap<>();
        Map<UUID, Set<UUID>> likedCategories = new HashMap<>();
        Map<UUID, Set<UUID>> dislikedCategories = new HashMap<>();
        Map<UUID, Set<UUID>> standardAllergens = new HashMap<>();

        // 메뉴 선호: LIKE→선호 메뉴, EXCLUDE→제외 메뉴 (IN 쿼리 1번)
        QUserMenuEntity userMenu = QUserMenuEntity.userMenuEntity;
        List<Tuple> menuRows = queryFactory
                .select(userMenu.userId, userMenu.menuId, userMenu.preference)
                .from(userMenu)
                .where(userMenu.userId.in(userIds))
                .fetch();
        for (Tuple row : menuRows) {
            UUID userId = row.get(userMenu.userId);
            UUID menuId = row.get(userMenu.menuId);
            Map<UUID, Set<UUID>> target =
                    row.get(userMenu.preference) == UserMenuPreference.LIKE ? likedMenus : excludeMenus;
            target.computeIfAbsent(userId, k -> new HashSet<>()).add(menuId);
        }

        // 카테고리 선호: LIKE→선호, DISLIKE→불호(소프트 감점) (IN 쿼리 1번)
        QUserCategoryEntity userCategory = QUserCategoryEntity.userCategoryEntity;
        List<Tuple> categoryRows = queryFactory
                .select(userCategory.userId, userCategory.categoryId, userCategory.preference)
                .from(userCategory)
                .where(userCategory.userId.in(userIds))
                .fetch();
        for (Tuple row : categoryRows) {
            UUID userId = row.get(userCategory.userId);
            UUID categoryId = row.get(userCategory.categoryId);
            Map<UUID, Set<UUID>> target =
                    row.get(userCategory.preference) == UserCategoryPreference.LIKE
                            ? likedCategories : dislikedCategories;
            target.computeIfAbsent(userId, k -> new HashSet<>()).add(categoryId);
        }

        // 표준 알레르기(22종) (IN 쿼리 1번)
        QUserAllergenEntity userAllergen = QUserAllergenEntity.userAllergenEntity;
        List<Tuple> allergenRows = queryFactory
                .select(userAllergen.userId, userAllergen.allergenId)
                .from(userAllergen)
                .where(userAllergen.userId.in(userIds))
                .fetch();
        for (Tuple row : allergenRows) {
            standardAllergens
                    .computeIfAbsent(row.get(userAllergen.userId), k -> new HashSet<>())
                    .add(row.get(userAllergen.allergenId));
        }

        // userId 기준으로 최종 조립 (선호 없는 멤버도 빈 Set으로 포함)
        return userIds.stream()
                .map(userId -> new MemberPreference(
                        userId,
                        likedMenus.getOrDefault(userId, Set.of()),
                        likedCategories.getOrDefault(userId, Set.of()),
                        excludeMenus.getOrDefault(userId, Set.of()),
                        dislikedCategories.getOrDefault(userId, Set.of()),
                        standardAllergens.getOrDefault(userId, Set.of())
                ))
                .toList();
    }

    /**
     * 전체 메뉴를 카테고리·알레르기 매핑과 함께 조립한다.
     */
    @Override
    public List<MenuInfo> findAllMenus() {
        QMenuEntity menu = QMenuEntity.menuEntity;
        QMenuAllergenEntity menuAllergen = QMenuAllergenEntity.menuAllergenEntity;

        // 알레르기 없는 메뉴도 포함하도록 함
        // 이름순 정렬: 정렬을 생략하면 PK(=삽입) 순서로 돌아오는데, 마스터 시드가 카테고리별로
        // 묶여 있어 점수 동점 시 stable sort가 한 카테고리를 앞에 몰아준다.
        List<Tuple> rows = queryFactory
                .select(menu.id, menu.categoryId, menuAllergen.allergenId)
                .from(menu)
                .leftJoin(menuAllergen).on(menuAllergen.menuId.eq(menu.id))
                .orderBy(menu.name.asc())
                .fetch();

        Map<UUID, UUID> categoryByMenu = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> allergensByMenu = new HashMap<>();
        for (Tuple row : rows) {
            UUID menuId = row.get(menu.id);
            categoryByMenu.putIfAbsent(menuId, row.get(menu.categoryId));
            Set<UUID> allergens = allergensByMenu.computeIfAbsent(menuId, k -> new HashSet<>());
            UUID allergenId = row.get(menuAllergen.allergenId);
            if (allergenId != null) {   // left join이라 알레르기 없는 메뉴는 null
                allergens.add(allergenId);
            }
        }

        return categoryByMenu.entrySet().stream()
                .map(entry -> new MenuInfo(
                        entry.getKey(),
                        entry.getValue(),
                        allergensByMenu.getOrDefault(entry.getKey(), Set.of())))
                .toList();
    }

    /**
     * 그룹의 메뉴별 최근 선택 정보를 조립한다. 먹은 적 없는 메뉴는 맵에 없음 → 감점 0.
     */
    @Override
    public Map<UUID, Recency> findRecencyByGroupId(UUID groupId) {
        QVoteCandidateEntity voteCandidate = QVoteCandidateEntity.voteCandidateEntity;
        QVoteSessionEntity voteSession = QVoteSessionEntity.voteSessionEntity;
        DateTimeExpression<LocalDateTime> lastSelectedAt = voteSession.closedAt.max();

        // 선택 확정(selected) 후보 × 세션 조인 → 메뉴별 최근 마감일(MAX)
        List<Tuple> rows = queryFactory
                .select(voteCandidate.menuId, lastSelectedAt)
                .from(voteCandidate)
                .join(voteSession).on(voteSession.id.eq(voteCandidate.voteSessionId))
                .where(voteSession.diningGroupId.eq(groupId),
                        voteCandidate.selected.isTrue())
                .groupBy(voteCandidate.menuId)
                .fetch();

        LocalDate today = LocalDate.now();
        Map<UUID, Recency> result = new HashMap<>();
        for (Tuple row : rows) {
            LocalDateTime selectedAt = row.get(lastSelectedAt);
            if (selectedAt == null) {   // 세션이 아직 안 닫힘 → 최근성 없음으로 취급
                continue;
            }
            // 오늘까지 경과 일수
            long days = ChronoUnit.DAYS.between(selectedAt.toLocalDate(), today);
            result.put(row.get(voteCandidate.menuId), new Recency(days));
        }
        return result;
    }

    /**
     * 메뉴별 글로벌 인기 점수(0~1)를 조립한다.
     */
    @Override
    public Map<UUID, Double> findMenuPopularity() {
        QVoteCandidateEntity voteCandidate = QVoteCandidateEntity.voteCandidateEntity;
        NumberExpression<Long> selectionCount = voteCandidate.count();

        // 선택 확정(selected) 횟수를 메뉴별로 집계
        List<Tuple> rows = queryFactory
                .select(voteCandidate.menuId, selectionCount)
                .from(voteCandidate)
                .where(voteCandidate.selected.isTrue())
                .groupBy(voteCandidate.menuId)
                .fetch();

        if (rows.isEmpty()) {   // 선택 이력 없음 → 인기 가중 0
            return Map.of();
        }

        // 최댓값으로 0~1 정규화
        long max = rows.stream()
                .mapToLong(row -> row.get(selectionCount))
                .max()
                .orElse(1L);

        Map<UUID, Double> result = new HashMap<>();
        for (Tuple row : rows) {
            result.put(row.get(voteCandidate.menuId), (double) row.get(selectionCount) / max);
        }
        return result;
    }
}
