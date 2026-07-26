package com.gm.db.domain.history;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.history.model.PreviousHistoryRecord;
import com.gm.core.domain.history.repository.PreviousHistoryRepository;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.db.domain.group.entity.QDiningGroupEntity;
import com.gm.db.domain.group.entity.QGroupMemberEntity;
import com.gm.db.domain.store.entity.QStoreEntity;
import com.gm.db.domain.vote.candidate.entity.QVoteCandidateEntity;
import com.gm.db.domain.vote.session.entity.QVoteSessionEntity;

/** QueryDSL 조인 쿼리로 지난 기록을 조회하는 저장소 구현체다. */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreviousHistoryRepositoryImpl implements PreviousHistoryRepository {

    private static final QGroupMemberEntity GROUP_MEMBER =
            QGroupMemberEntity.groupMemberEntity;
    private static final QDiningGroupEntity DINING_GROUP =
            QDiningGroupEntity.diningGroupEntity;
    private static final QVoteSessionEntity VOTE_SESSION =
            QVoteSessionEntity.voteSessionEntity;
    private static final QStoreEntity RESTAURANT = QStoreEntity.storeEntity;
    private static final QVoteCandidateEntity VOTE_CANDIDATE =
            QVoteCandidateEntity.voteCandidateEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PreviousHistoryRecord> findPreviousHistoryByUserId(UUID userId) {
        return createHistoryQuery(userId)
                .orderBy(VOTE_SESSION.closedAt.desc(), VOTE_SESSION.id.desc())
                .fetch();
    }

    @Override
    public Optional<PreviousHistoryRecord> findPreviousHistoryByUserIdAndVoteSessionId(
            UUID userId,
            UUID voteSessionId
    ) {
        return Optional.ofNullable(
                createHistoryQuery(userId)
                        .where(VOTE_SESSION.id.eq(voteSessionId))
                        .fetchOne()
        );
    }

    private JPAQuery<PreviousHistoryRecord> createHistoryQuery(UUID userId) {
        return queryFactory
                .select(historyProjection())
                .from(GROUP_MEMBER)
                .join(DINING_GROUP)
                .on(DINING_GROUP.id.eq(GROUP_MEMBER.diningGroupId))
                .join(VOTE_SESSION)
                .on(VOTE_SESSION.diningGroupId.eq(DINING_GROUP.id))
                .join(RESTAURANT)
                .on(RESTAURANT.voteSessionId.eq(VOTE_SESSION.id),
                        RESTAURANT.selected.isTrue())
                .join(VOTE_CANDIDATE)
                .on(VOTE_CANDIDATE.voteSessionId.eq(VOTE_SESSION.id),
                        VOTE_CANDIDATE.selected.isTrue())
                .where(GROUP_MEMBER.userId.eq(userId),
                        GROUP_MEMBER.status.eq(GroupMemberStatus.ACTIVE),
                        VOTE_SESSION.voteSessionStatus.eq(VoteSessionStatus.COMPLETED));
    }

    private ConstructorExpression<PreviousHistoryRecord> historyProjection() {
        return Projections.constructor(
                PreviousHistoryRecord.class,
                DINING_GROUP.id,
                DINING_GROUP.name,
                VOTE_SESSION.id,
                RESTAURANT.name,
                RESTAURANT.url,
                RESTAURANT.address,
                RESTAURANT.latitude,
                RESTAURANT.longitude,
                RESTAURANT.distance,
                RESTAURANT.externalPlaceId,
                VOTE_CANDIDATE.goCount.coalesce(0),
                VOTE_CANDIDATE.maybeCount.coalesce(0),
                VOTE_CANDIDATE.noCount.coalesce(0),
                VOTE_SESSION.closedAt
        );
    }
}
