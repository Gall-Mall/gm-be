package com.gm.api.controller.invite;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.invite.dto.response.GroupMemberResponse;
import com.gm.api.controller.invite.dto.response.InviteCreateResponse;
import com.gm.api.controller.invite.dto.response.InviteInfoResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.invite.service.InviteService;

/**
 * 초대 코드 생성·조회·가입 API(INVITE-001/002/003)를 제공한다.
 *
 * <p>세 엔드포인트 모두 실제 JWT 인증({@code @AuthenticationPrincipal})을 요구한다. Group
 * 컨트롤러의 임시 {@code X-User-Id} 헤더 패턴을 따르지 않는 이유는, 이 도메인의 rate
 * limiting이 사용자별로 의미를 가지려면 userId가 스푸핑 불가능해야 하기 때문이다.</p>
 */
@RestController
@Slf4j
public class InviteController {

    private final InviteService inviteService;
    private final String inviteBaseUrl;

    /**
     * {@code @Value}는 Lombok {@code @RequiredArgsConstructor}로 주입할 수 없어 생성자를
     * 직접 작성한다.
     */
    public InviteController(
            InviteService inviteService,
            @Value("${app.invite.base-url}") String inviteBaseUrl
    ) {
        this.inviteService = inviteService;
        this.inviteBaseUrl = inviteBaseUrl;
    }

    /**
     * 초대 코드 생성 (INVITE-001)
     *
     * <p>그룹장만 호출할 수 있다. 그룹 정원이 찬 경우 새 초대 코드를 발급하지 않는다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param groupId 초대 코드를 발급할 그룹 식별자
     * @return 생성된 초대 코드와 초대 링크
     */
    @PostMapping("/api/groups/{groupId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEnvelope<InviteCreateResponse> create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId
    ) {
        String inviteCode = inviteService.create(groupId, principal.getUserId());
        log.info("invite code is {}", inviteCode);
        return ResponseEnvelope.success(InviteCreateResponse.of(inviteCode, inviteBaseUrl));
    }

    /**
     * 초대 정보 조회 (INVITE-002)
     *
     * <p>초대 코드가 가리키는 그룹 정보와 가입 가능 여부를 조회한다. 인증된 회원이면 누구나
     * 호출할 수 있다(방장 권한 불필요). 인증 자체는 {@code SecurityConfig}가 강제하므로
     * 이 메서드는 인증 주체를 별도로 필요로 하지 않는다.</p>
     *
     * @param inviteCode 조회할 초대 코드
     * @return 그룹 정보와 가입 가능 여부
     */
    @GetMapping("/api/invites/{inviteCode}")
    public ResponseEnvelope<InviteInfoResponse> getInfo(@PathVariable String inviteCode) {
        log.info("get invite code is {}", inviteCode);
        return ResponseEnvelope.success(InviteInfoResponse.from(inviteService.getInfo(inviteCode)));
    }

    /**
     * 초대 코드로 그룹 가입 (INVITE-003)
     *
     * <p>요청 회원을 MEMBER 역할·ACTIVE 상태의 그룹원으로 등록한다. 그룹 정원 확인과
     * 가입 저장은 하나의 원자적 흐름으로 처리된다.</p>
     *
     * @param principal 요청 회원 (인증 주체)
     * @param inviteCode 사용할 초대 코드
     * @return 등록된 멤버십 정보
     */
    @PostMapping("/api/invites/{inviteCode}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEnvelope<GroupMemberResponse> join(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable String inviteCode
    ) {
        log.info("컨트롤러 들어옴");
        GroupMember member = inviteService.join(inviteCode, principal.getUserId());
        log.info("member is {}", member);
        return ResponseEnvelope.success(GroupMemberResponse.from(member));
    }
}
