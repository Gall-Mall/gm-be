package com.gm.api.controller.group.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * 그룹장 위임 요청이다.
 *
 * @param userId 새 그룹장이 될 활성 멤버의 회원 식별자
 */
public record GroupOwnerTransferRequest(
        @NotNull UUID userId
) {
}
