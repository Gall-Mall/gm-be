package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;

/**
 * 메뉴 투표 후보의 JPA 저장과 화면용 조회를 제공한다.
 */
public interface VoteCandidateJpaRepository extends JpaRepository<VoteCandidateEntity, UUID> {

    /**
     * 후보와 메뉴·카테고리를 한 번에 조회해 투표 화면 모델로 만든다.
     * 아직 집계되지 않은 nullable 카운트는 0으로 바꾸고 노출 순서를 유지한다.
     *
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 노출 순서대로 정렬된 메뉴 후보
     */
    @Query("""
            select new com.gm.core.domain.vote.candidate.model.MenuVoteCandidate(
                candidate.id,
                candidate.voteSessionId,
                candidate.menuId,
                menu.categoryId,
                menu.name,
                category.name,
                menu.imageUrl,
                candidate.displayOrder,
                coalesce(candidate.goCount, 0),
                coalesce(candidate.maybeCount, 0),
                coalesce(candidate.noCount, 0),
                coalesce(candidate.respondentCount, 0),
                candidate.resultStatus,
                candidate.description
            )
            from VoteCandidateEntity candidate
            join MenuEntity menu on menu.id = candidate.menuId
            join FoodCategoryEntity category on category.id = menu.categoryId
            where candidate.voteSessionId = :voteSessionId
            order by candidate.displayOrder asc
            """)
    List<MenuVoteCandidate> findMenuVoteCandidates(
            @Param("voteSessionId") UUID voteSessionId
    );
}
