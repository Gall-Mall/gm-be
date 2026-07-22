package com.gm.core.domain.group.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gm.core.domain.group.model.Group;
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
}
