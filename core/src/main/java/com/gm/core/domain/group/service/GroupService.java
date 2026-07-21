package com.gm.core.domain.group.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
     * 요청 회원이 활성 멤버로 참여 중인 그룹 목록을 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @return 참여 중인 그룹 목록
     */
    @Transactional(readOnly = true)
    public List<Group> findMyGroups(UUID userId) {
        log.info("내 그룹 목록 조회: userId: {}", userId);
        return groupRepository.findAllByMemberUserId(userId);
    }
}
