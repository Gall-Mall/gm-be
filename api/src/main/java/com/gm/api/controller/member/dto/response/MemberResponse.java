package com.gm.api.controller.member.dto.response;

import com.gm.core.domain.member.model.Member;

public record MemberResponse(
        String name,
        String phoneNumber
) {
    public static MemberResponse from(Member member){
        return new MemberResponse(member.name(), member.phoneNumber());
    }
}
