package com.gm.api.controller.group;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.group.dto.request.GroupCreateRequest;
import com.gm.api.controller.group.dto.response.GroupResponse;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.service.GroupService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    /**
     * 그룹 생성 (GROUP-001)
     *
     * <p>그룹을 생성하고 요청 회원을 OWNER 역할·ACTIVE 상태의 멤버로 등록한다.</p>
     *
     * @param userId 요청 회원 식별자 (AUTH-001 구현 전 임시 헤더, 이후 인증 주체로 대체)
     * @param request 그룹 생성 요청
     * @return 생성된 그룹 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEnvelope<GroupResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody GroupCreateRequest request
    ) {
        Group group = groupService.create(request.toNewGroup(userId));
        return ResponseEnvelope.success(GroupResponse.from(group));
    }
}
