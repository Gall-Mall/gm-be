package com.gm.db.domain.group;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupMemberStatus;
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

    @Override
    public Page<Group> findAllByMemberUserId(UUID userId, Pageable pageable) {
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return groupJpaRepository
                .findAllWithMemberCountByMemberUserIdAndStatus(userId, GroupMemberStatus.ACTIVE, pageOnly)
                .map(groupMapper::toDomainModel);
    }
}
