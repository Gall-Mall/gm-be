package com.gm.api.controller.invite.dto.response;

/**
 * 초대 코드 생성(INVITE-001) 응답이다.
 *
 * @param inviteCode 생성된 초대 코드
 * @param inviteUrl 초대 코드를 포함해 조립한 공유용 링크 (별도로 저장하지 않고 응답 시점에 조립한다)
 */
public record InviteCreateResponse(
        String inviteCode,
        String inviteUrl
) {

    /**
     * 초대 코드와 base URL을 조합해 응답을 만든다.
     *
     * @param inviteCode 생성된 초대 코드
     * @param baseUrl 초대 링크의 기준이 되는 서비스 base URL
     * @return 조립된 응답
     */
    public static InviteCreateResponse of(String inviteCode, String baseUrl) {
        return new InviteCreateResponse(inviteCode, baseUrl + "/invites/" + inviteCode);
    }
}
