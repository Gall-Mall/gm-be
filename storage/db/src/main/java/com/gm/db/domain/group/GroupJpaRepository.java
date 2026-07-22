package com.gm.db.domain.group;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.GroupMemberStatus;

@Repository
public interface GroupJpaRepository extends JpaRepository<GroupEntity, UUID> {

    /**
     * groupId에 해당하는 그룹을, 요청 회원이 특정 상태의 멤버인 경우에 한해 멤버 수·요청 회원 역할과
     * 함께 단일 쿼리로 조회한다.
     *
     * <p>멤버 수는 상관 서브쿼리로 함께 집계해, 그룹 조회와 멤버 수 조회를 별도 쿼리로 나누지 않는다.
     * 요청 회원이 활성 멤버가 아니면(그룹이 없거나, 있어도 멤버가 아니거나) 빈 값을 반환한다 —
     * 이 두 경우를 구분하려면 {@link #existsById(UUID)}를 별도로 호출해야 한다.</p>
     */
    @Query("""
            select new com.gm.db.domain.group.GroupDetailProjection(
                g.id, g.ownerUserId, g.name, g.locationAddress, g.latitude, g.longitude,
                g.searchRadiusM, g.recommendationTime, g.maxMemberCount,
                (select count(m2) from GroupMemberEntity m2
                        where m2.diningGroupId = g.id and m2.status = :status),
                g.createdAt, g.updatedAt, m.role
            )
            from GroupEntity g
            join GroupMemberEntity m on m.diningGroupId = g.id
            where g.id = :groupId and m.userId = :userId and m.status = :status
            """)
    Optional<GroupDetailProjection> findDetailByIdAndMemberUserIdAndStatus(
            @Param("groupId") UUID groupId,
            @Param("userId") UUID userId,
            @Param("status") GroupMemberStatus status
    );
}
