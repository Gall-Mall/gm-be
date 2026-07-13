package com.gm.api.controller.member;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.member.dto.response.MemberResponse;
import com.gm.core.domain.member.model.Member;
import com.gm.core.domain.member.service.MemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * 멤버 조회 예시
     * @param memberId
     * @return
     */
    @GetMapping("/{memberId}")
    public ResponseEnvelope<MemberResponse> findById(@PathVariable UUID memberId){
        Member member = memberService.findById(memberId);
        return ResponseEnvelope.success(MemberResponse.from(member));
    }
}
