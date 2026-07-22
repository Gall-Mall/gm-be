package com.gm.core.domain.group.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.NewGroup;

public interface GroupRepository {

    /**
     * 그룹을 저장하고 요청 회원을 OWNER 역할·ACTIVE 상태의 멤버로 함께 등록한다.
     *
     * <p>그룹과 그룹장 멤버 등록은 하나의 원자적 작업으로 처리한다.</p>
     *
     * @param newGroup 그룹 생성 명세
     * @return 저장된 그룹
     */
    Group create(NewGroup newGroup);

    /**
     * 요청 회원이 활성 멤버로 참여 중인 그룹 목록을 페이지 단위 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @param pageable 페이지 번호·크기 (정렬 기준은 항상 최근 투표 세션 활동순으로 고정된다.
     *         투표 세션이 없는 그룹은 뒤로 밀려 그룹 생성일 내림차순으로 정렬된다)
     * @return 참여 중인 그룹 페이지 (없으면 빈 페이지)
     */
    Page<Group> findAllByMemberUserId(UUID userId, Pageable pageable);

    /**
     * groupId에 해당하는 그룹 정보를, 요청 회원이 그 그룹의 활성 멤버인 경우에 한해 역할과 함께 조회한다.
     *
     * <p>요청 회원이 활성 멤버가 아니면(그룹이 없거나, 있어도 멤버가 아니거나) 빈 값을 반환한다.
     * 그룹 자체의 존재 여부는 {@link #existsById(UUID)}로 별도 확인해야 한다.</p>
     *
     * @param groupId 조회할 그룹 식별자
     * @param userId 요청 회원 식별자
     * @return 그룹과 요청 회원의 역할 (활성 멤버가 아니면 빈 값)
     */
    Optional<GroupDetail> findDetailByIdAndMemberUserId(UUID groupId, UUID userId);

    /**
     * groupId에 해당하는 그룹이 존재하는지 확인한다.
     *
     * @param groupId 확인할 그룹 식별자
     * @return 존재하면 {@code true}
     */
    boolean existsById(UUID groupId);
}
