package com.gm.db.domain.group;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.GroupMemberStatus;

@Repository
public interface GroupJpaRepository extends JpaRepository<GroupEntity, UUID> {

    /**
     * 요청 회원이 특정 상태의 멤버로 참여 중인 그룹을 멤버 수와 함께 단일 쿼리로 페이지 조회한다.
     *
     * <p>멤버 수는 상관 서브쿼리로 함께 집계해, 그룹 조회와 멤버 수 조회를 별도 쿼리로
     * 나누지 않는다. {@code pageable}은 페이지 번호·크기 산정에만 쓰인다(전달된
     * {@code Sort}는 무시된다).</p>
     *
     * <p>정렬은 그룹별 가장 최근 투표 세션 생성 시각({@code vote_session.created_at}) 내림차순이
     * 우선이고, 투표 세션이 하나도 없는 그룹은 뒤로 밀려 그룹 생성일({@code g.createdAt}) 내림차순으로
     * 정렬된다.</p>
     */
    @Query(
            value = """
                    select new com.gm.db.domain.group.GroupSummaryProjection(
                        g.id, g.ownerUserId, g.name, g.locationAddress, g.latitude, g.longitude,
                        g.searchRadiusM, g.recommendationTime, g.maxMemberCount,
                        (select count(m2) from GroupMemberEntity m2
                                where m2.diningGroupId = g.id and m2.status = :status),
                        g.createdAt, g.updatedAt
                    )
                    from GroupEntity g
                    join GroupMemberEntity m on m.diningGroupId = g.id
                    where m.userId = :userId and m.status = :status
                    order by
                        (select max(vs.createdAt) from VoteSessionEntity vs where vs.diningGroupId = g.id) desc nulls last,
                        g.createdAt desc
                    """,
            countQuery = """
                    select count(g)
                    from GroupEntity g
                    join GroupMemberEntity m on m.diningGroupId = g.id
                    where m.userId = :userId and m.status = :status
                    """
    )
    Page<GroupSummaryProjection> findAllWithMemberCountByMemberUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") GroupMemberStatus status,
            Pageable pageable
    );
}
