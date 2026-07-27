package com.gm.db.domain.group.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.db.domain.group.entity.GroupMemberEntity;

@Repository
public interface GroupMemberJpaRepository extends JpaRepository<GroupMemberEntity, UUID> {

    /**
     * 그룹에 해당 회원의 멤버십 행을 상태 무관하게 조회한다. 유니크 제약(그룹당 회원 한 명에
     * 행 하나)이 상태를 구분하지 않으므로, 가입 처리 시 이 행의 상태(ACTIVE/LEFT/KICKED)를
     * 직접 확인해 재가입 가능 여부를 판단해야 한다.
     */
    Optional<GroupMemberEntity> findByDiningGroupIdAndUserId(UUID diningGroupId, UUID userId);

    /** 그룹의 특정 상태 멤버 수를 센다. 정원 확인에 사용한다. */
    long countByDiningGroupIdAndStatus(UUID diningGroupId, GroupMemberStatus status);

    /** 요청 회원이 해당 그룹의 특정 역할·상태 멤버인지 확인한다. 방장 전용 작업 권한 검사에 사용한다. */
    boolean existsByDiningGroupIdAndUserIdAndRoleAndStatus(
            UUID diningGroupId, UUID userId, GroupMemberRole role, GroupMemberStatus status);

    /** 그룹의 활성 멤버와 사용자 표시 정보를 가입 순서대로 조회한다. */
    @Query("""
            select gm.id as groupMemberId,
                   gm.userId as userId,
                   coalesce(nullif(u.nickname, ''), u.name, '멤버') as name,
                   coalesce(u.email, '') as email,
                   gm.role as role,
                   gm.status as status
              from GroupMemberEntity gm
              left join UserEntity u on u.id = gm.userId
             where gm.diningGroupId = :groupId
               and gm.status = :status
             order by gm.joinedAt asc
            """)
    List<GroupMemberProfileProjection> findProfilesByGroupIdAndStatus(
            @Param("groupId") UUID groupId,
            @Param("status") GroupMemberStatus status
    );

}
