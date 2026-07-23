package com.gm.core.domain.invite.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface InviteRepository {

    /**
     * 초대 코드가 존재하지 않을 때만 groupId를 값으로 저장한다(SETNX와 동일한 원자적 연산).
     *
     * @param inviteCode 저장할 초대 코드
     * @param groupId 초대 코드가 가리킬 그룹 식별자
     * @param ttl 초대 코드의 유효 기간
     * @return 저장에 성공하면 {@code true}, 이미 같은 코드가 존재하면 {@code false}
     */
    boolean save(String inviteCode, UUID groupId, Duration ttl);

    /**
     * 초대 코드가 가리키는 그룹 식별자를 조회한다.
     *
     * @param inviteCode 조회할 초대 코드
     * @return 그룹 식별자 (코드가 없거나 만료됐으면 빈 값)
     */
    Optional<UUID> findGroupIdByCode(String inviteCode);
}
