package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import com.gm.db.domain.vote.candidate.mapper.VoteCandidateMapper;

/**
 * 메뉴 투표 후보 저장소를 JPA로 구현한다.
 */
@Repository
@RequiredArgsConstructor
public class VoteCandidateRepositoryImpl implements VoteCandidateRepository {

    private final VoteCandidateJpaRepository voteCandidateJpaRepository;
    private final VoteCandidateMapper voteCandidateMapper;

    @Override
    public List<VoteCandidate> saveAll(List<VoteCandidate> candidates) {
        List<VoteCandidateEntity> entities = candidates.stream()
                .map(voteCandidateMapper::toEntity)
                .toList();
        return voteCandidateJpaRepository.saveAll(entities).stream()
                .map(voteCandidateMapper::toDomain)
                .toList();
    }

    @Override
    public List<MenuVoteCandidate> findAllByVoteSessionId(UUID voteSessionId) {
        return voteCandidateJpaRepository.findMenuVoteCandidates(voteSessionId);
    }
}
