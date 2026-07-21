package com.gm.db.domain.group;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository groupJpaRepository;
    private final GroupMemberJpaRepository groupMemberJpaRepository;
    private final GroupMapper groupMapper;

    @Override
    public Group create(NewGroup newGroup) {
        GroupEntity group = groupJpaRepository.save(groupMapper.toEntity(newGroup));
        groupMemberJpaRepository.save(GroupMemberEntity.ofOwner(group.getId(), newGroup.ownerUserId()));
        return groupMapper.toDomainModel(group, 1);
    }
}
