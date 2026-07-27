package com.gm.core.domain.group.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.model.GroupUpdate;
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

    /**
     * groupId에 해당하는 그룹을 멤버 수와 함께 조회한다. 요청 회원의 멤버십 여부와 무관하게 조회한다.
     *
     * @param groupId 조회할 그룹 식별자
     * @return 그룹 정보 (없으면 빈 값)
     */
    Optional<Group> findById(UUID groupId);

    /**
     * groupId 그룹 행을 잠근 뒤 정원 확인과 멤버 등록을 하나의 원자적 흐름으로 수행한다.
     *
     * <p>기존 멤버십 행의 상태에 따라 결과가 달라진다:</p>
     * <ul>
     *     <li>이미 {@code ACTIVE} 멤버라면 빈 값을 반환해, 호출자가 "이미 가입함"의 의미를
     *         자신의 도메인 맥락에 맞게 해석하도록 한다.</li>
     *     <li>{@code LEFT} 상태였다면 정원을 다시 확인한 뒤 같은 행을 {@code ACTIVE}로
     *         재활성화한다(자발적 탈퇴자는 재가입을 허용한다).</li>
     *     <li>{@code KICKED} 상태였다면 정원과 무관하게 재가입을 거부한다.</li>
     * </ul>
     *
     * @param groupId 가입할 그룹 식별자
     * @param userId 가입할 회원 식별자
     * @return 등록(또는 재활성화)된 멤버십 (이미 활성 멤버였다면 빈 값)
     * @throws com.gm.core.domain.group.exception.GroupException groupId에 해당하는 그룹이 없으면
     *         {@code GROUP_NOT_FOUND}, 정원이 가득 찼으면 {@code GROUP_FULL}, 강퇴 이력이 있으면
     *         {@code MEMBER_KICKED}
     */
    Optional<GroupMember> addActiveMember(UUID groupId, UUID userId);

    /**
     * 요청 회원이 groupId 그룹의 활성(ACTIVE) 상태 방장(OWNER) 멤버인지 확인한다.
     *
     * @param groupId 확인할 그룹 식별자
     * @param userId 요청 회원 식별자
     * @return 활성 방장 멤버이면 {@code true}
     */
    boolean isActiveOwner(UUID groupId, UUID userId);

    /**
     * groupId에 해당하는 그룹의 설정 전체를 groupUpdate 내용으로 교체한다.
     *
     * <p>그룹 행을 비관적 쓰기 잠금으로 조회해 {@link #addActiveMember(UUID, UUID)}와 같은 락
     * 정책으로 동시 가입 처리와 직렬화하며, 현재 활성 멤버 수보다 작은 {@code maxMemberCount}로는
     * 변경할 수 없다.</p>
     *
     * @param groupId 수정할 그룹 식별자
     * @param groupUpdate 교체할 그룹 설정
     * @return 수정된 그룹
     * @throws com.gm.core.domain.group.exception.GroupException groupId에 해당하는 그룹이 없으면
     *         {@code GROUP_NOT_FOUND}, 정원을 현재 활성 멤버 수보다 작게 변경하려 하면
     *         {@code GROUP_CAPACITY_BELOW_ACTIVE_MEMBERS}
     */
    Group update(UUID groupId, GroupUpdate groupUpdate);

    /**
     * groupId에 해당하는 그룹을 삭제한다.
     *
     * <p>그룹에 딸린 멤버십·투표 세션·후보·추천 식당은 DB의 {@code ON DELETE CASCADE}로 함께
     * 삭제된다. 권한 확인은 호출자(서비스)가 담당한다.</p>
     *
     * @param groupId 삭제할 그룹 식별자
     * @throws com.gm.core.domain.group.exception.GroupException groupId에 해당하는 그룹이 없으면
     *         {@code GROUP_NOT_FOUND}
     */
    void deleteById(UUID groupId);
}
