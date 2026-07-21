package com.gm.core.domain.group.service;

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
}
