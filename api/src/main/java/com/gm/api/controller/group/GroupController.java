package com.gm.api.controller.group;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.PageResponse;
import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.group.dto.request.GroupCreateRequest;
import com.gm.api.controller.group.dto.request.GroupUpdateRequest;
import com.gm.api.controller.group.dto.response.GroupDetailResponse;
import com.gm.api.controller.group.dto.response.GroupResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
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
     * @param principal 요청 회원 (인증 주체)
     * @param request 그룹 생성 요청
     * @return 생성된 그룹 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEnvelope<GroupResponse> create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody GroupCreateRequest request
    ) {
        Group group = groupService.create(request.toNewGroup(principal.getUserId()));
        return ResponseEnvelope.success(GroupResponse.from(group));
    }

    /**
     * 내 그룹 목록 조회 (GROUP-002)
     *
     * <p>요청 회원이 활성 멤버로 참여 중인 그룹 목록을 페이지 단위 조회한다. 정렬 기준은 항상
     * 그룹별 최근 투표 세션 활동순으로 고정되며(세션이 없으면 그룹 생성일 내림차순), 요청의
     * {@code sort} 파라미터는 반영되지 않는다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param pageable 페이지 번호·크기 (기본 0페이지, 20건)
     * @return 참여 중인 그룹 페이지 (없으면 빈 목록)
     */
    @GetMapping
    public ResponseEnvelope<PageResponse<GroupResponse>> findMyGroups(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<Group> groups = groupService.findMyGroups(principal.getUserId(), pageable);
        return ResponseEnvelope.success(PageResponse.from(groups.map(GroupResponse::from)));
    }

    /**
     * 그룹 상세 조회 (GROUP-003)
     *
     * <p>그룹 정보와 요청 회원의 그룹 내 역할을 조회한다. 요청 회원이 해당 그룹의 활성
     * 멤버가 아니면 접근할 수 없다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param groupId 조회할 그룹 식별자
     * @return 그룹 정보와 요청 회원의 역할
     */
    @GetMapping("/{groupId}")
    public ResponseEnvelope<GroupDetailResponse> findGroupDetail(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId
    ) {
        GroupDetail groupDetail = groupService.findGroupDetail(groupId, principal.getUserId());
        return ResponseEnvelope.success(GroupDetailResponse.from(groupDetail));
    }

    /**
     * 그룹 정보 수정 (GROUP-004)
     *
     * <p>그룹 이름·위치·추천 설정 전체를 요청 본문 값으로 교체한다. 그룹장만 수정할 수 있다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param groupId 수정할 그룹 식별자
     * @param request 그룹 수정 요청
     * @return 수정된 그룹 정보
     */
    @PutMapping("/{groupId}")
    public ResponseEnvelope<GroupResponse> update(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupUpdateRequest request
    ) {
        Group group = groupService.update(groupId, principal.getUserId(), request.toGroupUpdate());
        return ResponseEnvelope.success(GroupResponse.from(group));
    }

    /**
     * 그룹 삭제 (GROUP-005)
     *
     * <p>그룹과 딸린 멤버십·투표 세션·후보·추천 식당을 함께 삭제한다. 그룹장만 삭제할 수 있으며
     * 되돌릴 수 없다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param groupId 삭제할 그룹 식별자
     */
    @DeleteMapping("/{groupId}")
    public ResponseEnvelope<Void> delete(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId
    ) {
        groupService.delete(groupId, principal.getUserId());
        return ResponseEnvelope.success(null);
    }
}
