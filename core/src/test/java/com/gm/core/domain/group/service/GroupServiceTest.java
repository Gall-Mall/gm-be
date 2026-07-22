package com.gm.core.domain.group.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        Pageable pageable = PageRequest.of(0, 20);
        Page<Group> expected = new PageImpl<>(List.of(savedGroup(UUID.randomUUID(), newGroup)), pageable, 1);

        given(groupRepository.findAllByMemberUserId(userId, pageable)).willReturn(expected);

        Page<Group> actual = groupService.findMyGroups(userId, pageable);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("참여 중인 그룹이 없으면 빈 페이지를 반환한다")
    void findMyGroups_withNoMemberships_returnsEmptyPage() {
        groupService = new GroupService(groupRepository);

        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        given(groupRepository.findAllByMemberUserId(userId, pageable)).willReturn(Page.empty(pageable));

        Page<Group> actual = groupService.findMyGroups(userId, pageable);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("요청 회원이 활성 멤버이면 그룹 정보와 역할을 반환한다")
    void findGroupDetail_returnsGroupAndRole_whenRequesterIsActiveMember() {
        groupService = new GroupService(groupRepository);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Group group = savedGroup(groupId, newGroup(UUID.randomUUID()));
        GroupDetail expected = new GroupDetail(group, GroupMemberRole.MEMBER);

        given(groupRepository.findDetailByIdAndMemberUserId(groupId, userId))
                .willReturn(Optional.of(expected));

        GroupDetail actual = groupService.findGroupDetail(groupId, userId);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("groupId에 해당하는 그룹이 없으면 GROUP-001 오류를 던진다")
    void findGroupDetail_throwsGroupNotFound_whenGroupDoesNotExist() {
        groupService = new GroupService(groupRepository);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(groupRepository.findDetailByIdAndMemberUserId(groupId, userId))
                .willReturn(Optional.empty());
        given(groupRepository.existsById(groupId)).willReturn(false);

        assertThatThrownBy(() -> groupService.findGroupDetail(groupId, userId))
                .isInstanceOf(GroupException.class)
                .satisfies(exception -> assertThat(((GroupException) exception).getErrorCode().getCode())
                        .isEqualTo("GROUP-001"));
    }

    @Test
    @DisplayName("그룹은 존재하지만 요청 회원이 활성 멤버가 아니면 GROUP-002 오류를 던진다")
    void findGroupDetail_throwsNotGroupMember_whenRequesterIsNotActiveMember() {
        groupService = new GroupService(groupRepository);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(groupRepository.findDetailByIdAndMemberUserId(groupId, userId))
                .willReturn(Optional.empty());
        given(groupRepository.existsById(groupId)).willReturn(true);

        assertThatThrownBy(() -> groupService.findGroupDetail(groupId, userId))
                .isInstanceOf(GroupException.class)
                .satisfies(exception -> assertThat(((GroupException) exception).getErrorCode().getCode())
                        .isEqualTo("GROUP-002"));
    }
}
