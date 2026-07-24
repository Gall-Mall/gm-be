package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import com.gm.db.domain.menu.category.repository.FoodCategoryJpaRepository;
import com.gm.db.domain.menu.menu.entity.MenuEntity;
import com.gm.db.domain.menu.menu.repository.MenuJpaRepository;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import com.gm.db.domain.vote.candidate.mapper.MenuVoteCandidateMapper;
import com.gm.db.domain.vote.candidate.mapper.VoteCandidateMapper;

/**
 * 메뉴 투표 후보 저장소를 JPA로 구현한다.
 */
@Repository
@RequiredArgsConstructor
public class VoteCandidateRepositoryImpl implements VoteCandidateRepository {

    private final VoteCandidateJpaRepository voteCandidateJpaRepository;
    private final MenuJpaRepository menuJpaRepository;
    private final FoodCategoryJpaRepository foodCategoryJpaRepository;
    private final VoteCandidateMapper voteCandidateMapper;
    private final MenuVoteCandidateMapper menuVoteCandidateMapper;

    @Override
    public List<VoteCandidate> saveNewCandidates(List<VoteCandidate> candidates) {
        List<VoteCandidateEntity> entities = candidates.stream()
                .map(voteCandidateMapper::toEntity)
                .toList();
        return voteCandidateJpaRepository.saveAll(entities).stream()
                .map(voteCandidateMapper::toDomain)
                .toList();
    }

    @Override
    public List<MenuVoteCandidate> findAllByVoteSessionId(UUID voteSessionId) {
        return voteCandidateJpaRepository
                .findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId)
                .stream()
                .map(this::toMenuVoteCandidate)
                .toList();
    }

    private MenuVoteCandidate toMenuVoteCandidate(VoteCandidateEntity candidate) {
        MenuEntity menu = menuJpaRepository.findById(candidate.getMenuId())
                .orElseThrow(() -> new IllegalStateException(
                        "Menu not found: " + candidate.getMenuId()));
        FoodCategoryEntity category = foodCategoryJpaRepository.findById(menu.getCategoryId())
                .orElseThrow(() -> new IllegalStateException(
                        "Food category not found: " + menu.getCategoryId()));
        return menuVoteCandidateMapper.toDomain(candidate, menu, category);
    }
}
