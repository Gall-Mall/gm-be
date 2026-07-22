package com.gm.core.domain.invite.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.invite.exception.InviteErrorCode;
import com.gm.core.domain.invite.exception.InviteException;
import com.gm.core.domain.invite.model.InviteInfo;
import com.gm.core.domain.invite.repository.InviteRepository;
import com.gm.core.exception.CommonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link InviteService}의 성공·오류 분기와 초대 코드 생성 재시도 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupService groupService;

    private InviteService inviteService;

    private Group group(UUID groupId, UUID ownerUserId, int memberCount, int maxMemberCount) {
        LocalDateTime now = LocalDateTime.now();
        return new Group(
                groupId, ownerUserId, "점심팟", "서울특별시 강남구 테헤란로 123",
                37.5012345, 127.0398765, 1000, LocalTime.of(11, 0),
                maxMemberCount, memberCount, now, now
        );
    }

    @Test
    @DisplayName("그룹장이 정원이 남은 그룹에 초대 코드를 요청하면 6자리 코드를 생성해 저장하고 반환한다")
    void create_generatesAndSavesSixCharacterCode_whenRequesterIsOwnerAndNotFull() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, ownerUserId, 3, 6)));
        given(inviteRepository.save(anyString(), eq(groupId), any(Duration.class))).willReturn(true);

        String inviteCode = inviteService.create(groupId, ownerUserId);

        assertThat(inviteCode).matches("[0-9a-zA-Z]{6}");
        verify(inviteRepository).save(eq(inviteCode), eq(groupId), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("groupId에 해당하는 그룹이 없으면 GROUP-001 오류를 던진다")
    void create_throwsGroupNotFound_whenGroupDoesNotExist() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.create(groupId, UUID.randomUUID()))
                .isInstanceOf(GroupException.class)
                .satisfies(exception -> assertThat(((GroupException) exception).getErrorCode())
                        .isEqualTo(GroupErrorCode.GROUP_NOT_FOUND));
    }

    @Test
    @DisplayName("요청 회원이 그룹장이 아니면 GROUP-006 오류를 던진다")
    void create_throwsNotGroupOwner_whenRequesterIsNotOwner() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, ownerUserId, 3, 6)));

        assertThatThrownBy(() -> inviteService.create(groupId, requesterUserId))
                .isInstanceOf(GroupException.class)
                .satisfies(exception -> assertThat(((GroupException) exception).getErrorCode())
                        .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER));
    }

    @Test
    @DisplayName("그룹 정원이 가득 찼으면 GROUP-004 오류를 던지고 코드를 생성하지 않는다")
    void create_throwsGroupFull_whenGroupIsAtCapacity() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, ownerUserId, 6, 6)));

        assertThatThrownBy(() -> inviteService.create(groupId, ownerUserId))
                .isInstanceOf(GroupException.class)
                .satisfies(exception -> assertThat(((GroupException) exception).getErrorCode())
                        .isEqualTo(GroupErrorCode.GROUP_FULL));

        verify(inviteRepository, never()).save(anyString(), any(UUID.class), any(Duration.class));
    }

    @Test
    @DisplayName("생성한 코드가 이미 존재하면(SETNX 실패) 새 코드로 재시도한다")
    void create_retriesWithNewCode_whenGeneratedCodeCollides() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, ownerUserId, 3, 6)));
        // 첫 두 번은 충돌(false), 세 번째에 성공(true)
        given(inviteRepository.save(anyString(), eq(groupId), any(Duration.class)))
                .willReturn(false, false, true);

        String inviteCode = inviteService.create(groupId, ownerUserId);

        assertThat(inviteCode).matches("[0-9a-zA-Z]{6}");
        verify(inviteRepository, times(3)).save(anyString(), eq(groupId), any(Duration.class));
    }

    @Test
    @DisplayName("재시도 한도를 모두 소진하면 내부 오류로 처리한다")
    void create_throwsCommonException_whenRetriesExhausted() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, ownerUserId, 3, 6)));
        given(inviteRepository.save(anyString(), eq(groupId), any(Duration.class))).willReturn(false);

        assertThatThrownBy(() -> inviteService.create(groupId, ownerUserId))
                .isInstanceOf(CommonException.class);
    }

    @Test
    @DisplayName("유효한 초대 코드를 조회하면 그룹 정보와 가입 가능 여부를 반환한다")
    void getInfo_returnsGroupInfoAndJoinable_whenCodeIsValid() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        String inviteCode = "AB12CD";
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.of(groupId));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, UUID.randomUUID(), 3, 6)));

        InviteInfo info = inviteService.getInfo(inviteCode);

        assertThat(info.inviteCode()).isEqualTo(inviteCode);
        assertThat(info.groupId()).isEqualTo(groupId);
        assertThat(info.memberCount()).isEqualTo(3);
        assertThat(info.maxMemberCount()).isEqualTo(6);
        assertThat(info.joinable()).isTrue();
    }

    @Test
    @DisplayName("정원이 가득 찬 그룹이면 joinable=false를 반환한다")
    void getInfo_returnsNotJoinable_whenGroupIsAtCapacity() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        String inviteCode = "AB12CD";
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.of(groupId));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group(groupId, UUID.randomUUID(), 6, 6)));

        InviteInfo info = inviteService.getInfo(inviteCode);

        assertThat(info.joinable()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드를 조회하면 INVITE-001 오류를 던진다")
    void getInfo_throwsInviteInvalid_whenCodeDoesNotExist() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        String inviteCode = "ZZ99ZZ";
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.getInfo(inviteCode))
                .isInstanceOf(InviteException.class)
                .satisfies(exception -> assertThat(((InviteException) exception).getErrorCode())
                        .isEqualTo(InviteErrorCode.INVITE_INVALID));
    }

    @Test
    @DisplayName("유효한 초대 코드로 가입하면 Group 도메인에 위임해 등록된 멤버십을 반환한다")
    void join_delegatesToGroupService_andReturnsMembership() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String inviteCode = "AB12CD";
        LocalDateTime now = LocalDateTime.now();
        GroupMember expected = new GroupMember(
                UUID.randomUUID(), groupId, userId,
                GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE, now, now
        );
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.of(groupId));
        given(groupService.addMember(groupId, userId)).willReturn(Optional.of(expected));

        GroupMember actual = inviteService.join(inviteCode, userId);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드로 가입을 시도하면 INVITE-001 오류를 던진다")
    void join_throwsInviteInvalid_whenCodeDoesNotExist() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        String inviteCode = "ZZ99ZZ";
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.join(inviteCode, UUID.randomUUID()))
                .isInstanceOf(InviteException.class)
                .satisfies(exception -> assertThat(((InviteException) exception).getErrorCode())
                        .isEqualTo(InviteErrorCode.INVITE_INVALID));
    }

    @Test
    @DisplayName("이미 활성 멤버라 Group 도메인이 빈 값을 반환하면 INVITE-002 오류를 던진다")
    void join_throwsAlreadyMember_whenGroupServiceReportsExistingMembership() {
        inviteService = new InviteService(inviteRepository, groupRepository, groupService);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String inviteCode = "AB12CD";
        given(inviteRepository.findGroupIdByCode(inviteCode)).willReturn(Optional.of(groupId));
        given(groupService.addMember(groupId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.join(inviteCode, userId))
                .isInstanceOf(InviteException.class)
                .satisfies(exception -> assertThat(((InviteException) exception).getErrorCode())
                        .isEqualTo(InviteErrorCode.ALREADY_MEMBER));
    }
}
