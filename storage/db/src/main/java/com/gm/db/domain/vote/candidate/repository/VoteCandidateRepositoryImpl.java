package com.gm.db.domain.vote.candidate.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
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

    @Override
    public Optional<VoteCandidate> findSelectedCandidate(UUID voteSessionId) {
        return voteCandidateJpaRepository.findFirstByVoteSessionIdAndSelectedTrue(voteSessionId)
                .map(voteCandidateMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<MenuVoteResult> saveMenuVoteResults(
            UUID voteSessionId,
            List<MenuVoteResult> results
    ) {
        List<VoteCandidateEntity> candidates = voteCandidateJpaRepository
                .findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId);
        Map<UUID, MenuVoteResult> resultsByCandidateId = results.stream()
                .collect(Collectors.toMap(
                        result -> result.count().candidateId(),
                        Function.identity()
                ));
        // 일부 후보만 갱신되는 일을 막기 위해 Redis와 DB의 후보 집합을 먼저 대조한다.
        if (candidates.size() != resultsByCandidateId.size()) {
            throw new IllegalStateException("Menu vote snapshot does not match stored candidates");
        }
        List<MenuVoteResult> orderedResults = new ArrayList<>(candidates.size());
        for (VoteCandidateEntity candidate : candidates) {
            MenuVoteResult result = resultsByCandidateId.get(candidate.getId());
            if (result == null) {
                throw new IllegalStateException("Menu vote snapshot contains an unknown candidate");
            }
            candidate.finalizeMenuVote(result.count(), result.result());
            orderedResults.add(result);
        }
        voteCandidateJpaRepository.flush();
        return List.copyOf(orderedResults);
    }

    /** {@inheritDoc} */
    @Override
    public List<MenuVoteResult> findMenuVoteResults(UUID voteSessionId) {
        return voteCandidateJpaRepository
                .findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId)
                .stream()
                .map(candidate -> new MenuVoteResult(
                        new MenuVoteCount(
                                candidate.getId(),
                                candidate.getGoCount(),
                                candidate.getMaybeCount(),
                                candidate.getNoCount(),
                                candidate.getRespondentCount()
                        ),
                        candidate.getResultStatus()
                ))
                .toList();
    }

    @Override
    public List<UUID> findRemainingCandidateIdsForUpdate(UUID voteSessionId) {
        return voteCandidateJpaRepository.findAllByVoteSessionIdForUpdate(voteSessionId).stream()
                .filter(candidate -> candidate.getResultStatus() == VoteCandidateResult.CONFIRMED
                        || candidate.getResultStatus() == VoteCandidateResult.KEPT)
                .map(VoteCandidateEntity::getId)
                .toList();
    }

    @Override
    public VoteCandidate selectFinalCandidate(UUID voteSessionId, UUID candidateId) {
        List<VoteCandidateEntity> candidates = voteCandidateJpaRepository
                .findAllByVoteSessionIdForUpdate(voteSessionId);
        VoteCandidateEntity selected = candidates.stream()
                .filter(candidate -> candidate.getId().equals(candidateId))
                .filter(candidate -> candidate.getResultStatus() == VoteCandidateResult.CONFIRMED
                        || candidate.getResultStatus() == VoteCandidateResult.KEPT)
                .findFirst()
                .orElseThrow(() -> new VoteCandidateException(
                        VoteCandidateErrorCode.FINAL_MENU_SELECTION_NOT_ALLOWED));
        candidates.forEach(candidate -> candidate.updateSelected(candidate == selected));
        voteCandidateJpaRepository.flush();
        return voteCandidateMapper.toDomain(selected);
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
