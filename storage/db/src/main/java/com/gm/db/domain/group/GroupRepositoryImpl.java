package com.gm.db.domain.group;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
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
    public Optional<GroupDetail> findDetailByIdAndMemberUserId(UUID groupId, UUID userId) {
        return groupJpaRepository
                .findDetailByIdAndMemberUserIdAndStatus(groupId, userId, GroupMemberStatus.ACTIVE)
                .map(projection -> new GroupDetail(
                        groupMapper.toDomainModel(projection),
                        projection.currentUserRole()
                ));
    }

    @Override
    public boolean existsById(UUID groupId) {
        return groupJpaRepository.existsById(groupId);
    }
}
