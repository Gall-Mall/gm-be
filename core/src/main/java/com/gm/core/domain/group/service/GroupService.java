package com.gm.core.domain.group.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    /**
     * 그룹을 생성하고 요청 회원을 그룹장으로 등록한다.
     *
     * @param newGroup 그룹 생성 명세
     * @return 생성된 그룹
     */
    @Transactional
    public Group create(NewGroup newGroup) {
        log.info("그룹 생성: ownerUserId: {}, name: {}", newGroup.ownerUserId(), newGroup.name());
        return groupRepository.create(newGroup);
    }

    /**
     * 요청 회원이 활성 멤버로 참여 중인 그룹 목록을 페이지 단위 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @param pageable 페이지 번호·크기
     * @return 참여 중인 그룹 페이지
     */
    @Transactional(readOnly = true)
    public Page<Group> findMyGroups(UUID userId, Pageable pageable) {
        log.info("내 그룹 목록 조회: userId: {}, page: {}, size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());
        return groupRepository.findAllByMemberUserId(userId, pageable);
    }
}
