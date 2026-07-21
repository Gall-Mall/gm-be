package com.gm.core.domain.group.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    private GroupService groupService;

    private NewGroup newGroup(UUID ownerUserId) {
        return new NewGroup(
                ownerUserId,
                "점심팟",
                "서울특별시 강남구 테헤란로 123",
                37.5012345,
                127.0398765,
                1000,
                LocalTime.of(11, 0),
                6
        );
    }

    private Group savedGroup(UUID groupId, NewGroup newGroup) {
        LocalDateTime now = LocalDateTime.now();
        return new Group(
                groupId,
                newGroup.ownerUserId(),
                newGroup.name(),
                newGroup.locationAddress(),
                newGroup.latitude(),
                newGroup.longitude(),
                newGroup.searchRadiusM(),
                newGroup.recommendationTime(),
                newGroup.maxMemberCount(),
                1,
                now,
                now
        );
    }

    @Test
    @DisplayName("그룹 생성 요청을 그대로 리포지토리에 위임하고 저장 결과를 반환한다")
    void create_delegatesToRepository_andReturnsSavedGroup() {
        groupService = new GroupService(groupRepository);

        UUID ownerUserId = UUID.randomUUID();
        NewGroup newGroup = newGroup(ownerUserId);
        Group expected = savedGroup(UUID.randomUUID(), newGroup);

        given(groupRepository.create(newGroup)).willReturn(expected);

        Group actual = groupService.create(newGroup);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("리포지토리에 전달하는 생성 명세는 요청받은 값과 동일하다")
    void create_passesExactNewGroup_toRepository() {
        groupService = new GroupService(groupRepository);

        UUID ownerUserId = UUID.randomUUID();
        NewGroup newGroup = newGroup(ownerUserId);
        given(groupRepository.create(newGroup)).willReturn(savedGroup(UUID.randomUUID(), newGroup));

        groupService.create(newGroup);

        ArgumentCaptor<NewGroup> captor = ArgumentCaptor.forClass(NewGroup.class);
        verify(groupRepository).create(captor.capture());

        NewGroup captured = captor.getValue();
        assertThat(captured.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(captured.name()).isEqualTo("점심팟");
        assertThat(captured.locationAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(captured.latitude()).isEqualTo(37.5012345);
        assertThat(captured.longitude()).isEqualTo(127.0398765);
        assertThat(captured.searchRadiusM()).isEqualTo(1000);
        assertThat(captured.recommendationTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(captured.maxMemberCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("내 그룹 목록 조회 요청을 그대로 리포지토리에 위임하고 조회 결과를 반환한다")
    void findMyGroups_delegatesToRepository_andReturnsGroups() {
        groupService = new GroupService(groupRepository);

        UUID userId = UUID.randomUUID();
        NewGroup newGroup = newGroup(userId);
        List<Group> expected = List.of(savedGroup(UUID.randomUUID(), newGroup));

        given(groupRepository.findAllByMemberUserId(userId)).willReturn(expected);

        List<Group> actual = groupService.findMyGroups(userId);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("참여 중인 그룹이 없으면 빈 리스트를 반환한다")
    void findMyGroups_withNoMemberships_returnsEmptyList() {
        groupService = new GroupService(groupRepository);

        UUID userId = UUID.randomUUID();
        given(groupRepository.findAllByMemberUserId(userId)).willReturn(List.of());

        List<Group> actual = groupService.findMyGroups(userId);

        assertThat(actual).isEmpty();
    }
}
