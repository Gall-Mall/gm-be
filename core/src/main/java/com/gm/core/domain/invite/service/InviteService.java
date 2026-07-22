package com.gm.core.domain.invite.service;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.invite.exception.InviteErrorCode;
import com.gm.core.domain.invite.exception.InviteException;
import com.gm.core.domain.invite.model.InviteInfo;
import com.gm.core.domain.invite.repository.InviteRepository;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.CommonException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private static final Duration INVITE_CODE_TTL = Duration.ofHours(1);
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private final InviteRepository inviteRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    /**
     * 그룹장의 요청으로 그룹 가입에 사용할 초대 코드를 생성한다.
     *
     * @param groupId 초대 코드를 발급할 그룹 식별자
     * @param requesterUserId 요청 회원 식별자
     * @return 생성된 초대 코드
     * @throws GroupException groupId에 해당하는 그룹이 없어 {@code GROUP-001} 오류가 발생하는 경우
     * @throws GroupException 요청 회원이 그룹장이 아니어서 {@code GROUP-006} 오류가 발생하는 경우
     * @throws GroupException 그룹 정원이 가득 차 {@code GROUP-004} 오류가 발생하는 경우
     */
    @Transactional(readOnly = true)
    public String create(UUID groupId, UUID requesterUserId) {
        log.info("초대 코드 생성: groupId: {}, requesterUserId: {}", groupId, requesterUserId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (!group.ownerUserId().equals(requesterUserId)) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }
        if (group.memberCount() >= group.maxMemberCount()) {
            throw new GroupException(GroupErrorCode.GROUP_FULL);
        }

        return generateAndStoreCode(groupId);
    }

    /**
     * 초대 코드가 가리키는 그룹 정보와 가입 가능 여부를 조회한다.
     *
     * @param inviteCode 조회할 초대 코드
     * @return 그룹 정보와 가입 가능 여부
     * @throws InviteException 초대 코드가 없거나 만료돼 {@code INVITE-001} 오류가 발생하는 경우
     */
    @Transactional(readOnly = true)
    public InviteInfo getInfo(String inviteCode) {
        UUID groupId = resolveGroupId(inviteCode);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new InviteException(InviteErrorCode.INVITE_INVALID));

        boolean joinable = group.memberCount() < group.maxMemberCount();

        return new InviteInfo(
                inviteCode,
                group.id(),
                group.name(),
                group.memberCount(),
                group.maxMemberCount(),
                joinable
        );
    }

    /**
     * 초대 코드로 요청 회원을 그룹원으로 등록한다.
     *
     * @param inviteCode 사용할 초대 코드
     * @param userId 가입할 회원 식별자
     * @return 등록된 멤버십
     * @throws InviteException 초대 코드가 없거나 만료돼 {@code INVITE-001} 오류가 발생하는 경우
     * @throws InviteException 이미 활성 멤버여서 {@code INVITE-002} 오류가 발생하는 경우
     * @throws GroupException 그룹 정원이 가득 차 {@code GROUP-004} 오류가 발생하는 경우
     */
    public GroupMember join(String inviteCode, UUID userId) {
        UUID groupId = resolveGroupId(inviteCode);
        return groupService.addMember(groupId, userId)
                .orElseThrow(() -> new InviteException(InviteErrorCode.ALREADY_MEMBER));
    }

    /**
     * 초대 코드가 가리키는 그룹 식별자를 조회한다. 코드가 없거나 만료됐으면 예외를 던진다.
     */
    private UUID resolveGroupId(String inviteCode) {
        return inviteRepository.findGroupIdByCode(inviteCode)
                .orElseThrow(() -> new InviteException(InviteErrorCode.INVITE_INVALID));
    }

    /**
     * 초대 코드를 생성해 Redis에 원자적으로 저장한다. 이미 존재하는 코드와 충돌하면
     * (SETNX 실패) 새 코드로 재시도하며, 최대 시도 횟수를 넘기면 내부 오류로 처리한다.
     */
    private String generateAndStoreCode(UUID groupId) {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = InviteCodeGenerator.generate();
            if (inviteRepository.save(code, groupId, INVITE_CODE_TTL)) {
                return code;
            }
        }
        log.error("초대 코드 생성 재시도 소진: groupId: {}", groupId);
        throw new CommonException(CommonErrorCode.INTERNAL_ERROR);
    }
}
