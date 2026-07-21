package com.gm.db.domain.vote.repository;

import com.gm.core.domain.vote.model.VoteSession;
import com.gm.core.domain.vote.model.VoteSessionStatus;
import com.gm.core.domain.vote.repository.VoteSessionRepository;
import com.gm.db.domain.vote.entity.VoteSessionEntity;
import com.gm.db.domain.vote.mapper.VoteSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 투표 세션 저장소를 JPA로 구현한다.
 */
@Repository
@RequiredArgsConstructor
public class VoteSessionRepositoryImpl implements VoteSessionRepository {

    private final VoteSessionJPARepository voteSessionJPARepository;
    private final VoteSessionMapper voteSessionMapper;

    /** {@inheritDoc} */
    @Override
    public VoteSession save(VoteSession voteSession) {

        VoteSessionEntity voteSessionEntity = voteSessionMapper.toEntity(voteSession);
        VoteSessionEntity saved = voteSessionJPARepository.save(voteSessionEntity);

        return  voteSessionMapper.toDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> findById(UUID id) {
        return voteSessionJPARepository.findById(id)
                .map(voteSessionMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> updateStatus(
            UUID voteSessionId,
            VoteSessionStatus voteSessionStatus
    ) {
        return voteSessionJPARepository.findById(voteSessionId)
                .map(entity -> {
                    entity.updateStatus(voteSessionStatus);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public Optional<VoteSession> cancel(
            UUID voteSessionId,
            LocalDateTime cancelledAt
    ) {
        return voteSessionJPARepository.findById(voteSessionId)
                .map(entity -> {
                    entity.cancel(cancelledAt);
                    return voteSessionMapper.toDomain(entity);
                });
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID voteSessionId) {
        voteSessionJPARepository.deleteById(voteSessionId);
    }
}
