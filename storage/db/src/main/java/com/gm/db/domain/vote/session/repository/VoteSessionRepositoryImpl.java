package com.gm.db.domain.vote.session.repository;

import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import com.gm.db.domain.vote.session.mapper.VoteSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 투표 세션 저장소를 JPA로 구현한다.
 */
@Repository
@RequiredArgsConstructor
public class VoteSessionRepositoryImpl implements VoteSessionRepository {

    private final VoteSessionJpaRepository voteSessionJpaRepository;
    private final VoteSessionMapper voteSessionMapper;

    /** {@inheritDoc} */
    @Override
    public VoteSession save(VoteSession voteSession) {

        VoteSessionEntity voteSessionEntity = voteSessionMapper.toEntity(voteSession);
        VoteSessionEntity saved = voteSessionJpaRepository.save(voteSessionEntity);

        return  voteSessionMapper.toDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> findById(UUID id) {
        return voteSessionJpaRepository.findById(id)
                .map(voteSessionMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> findCurrentByGroupId(UUID diningGroupId) {
        return voteSessionJpaRepository
                .findFirstByDiningGroupIdAndVoteSessionStatusNotInOrderByCreatedAtDesc(
                        diningGroupId,
                        List.of(
                                VoteSessionStatus.COMPLETED,
                                VoteSessionStatus.CANCELLED,
                                VoteSessionStatus.FAILED
                        )
                )
                .map(voteSessionMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> findByIdForUpdate(UUID id) {
        return voteSessionJpaRepository.findByIdForUpdate(id)
                .map(voteSessionMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> updateStatus(
            UUID voteSessionId,
            VoteSessionStatus voteSessionStatus
    ) {
        return voteSessionJpaRepository.findById(voteSessionId)
                .map(entity -> {
                    entity.updateStatus(voteSessionStatus);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> startMenuVoting(UUID voteSessionId, LocalDateTime startedAt) {
        return voteSessionJpaRepository.findById(voteSessionId)
                .map(entity -> {
                    entity.startMenuVoting(startedAt);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public List<UUID> findExpiredMenuVoting(LocalDateTime cutoff, int limit) {
        return voteSessionJpaRepository
                .findAllByVoteSessionStatusAndStartedAtLessThanEqualOrderByStartedAtAsc(
                        VoteSessionStatus.MENU_VOTING,
                        cutoff,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(VoteSessionEntity::getId)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> cancel(
            UUID voteSessionId,
            LocalDateTime cancelledAt
    ) {
        return voteSessionJpaRepository.findById(voteSessionId)
                .map(entity -> {
                    entity.cancel(cancelledAt);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> complete(UUID voteSessionId, LocalDateTime completedAt) {
        return voteSessionJpaRepository.findById(voteSessionId)
                .map(entity -> {
                    entity.complete(completedAt);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID voteSessionId) {
        voteSessionJpaRepository.deleteById(voteSessionId);
    }
}
