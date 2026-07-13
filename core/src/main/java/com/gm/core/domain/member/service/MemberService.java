package com.gm.core.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.member.model.Member;
import com.gm.core.domain.member.repository.MemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member findById(UUID id){
        //member 에러 구현 요망
        log.info("member 조회: Id: {}", id);
        return memberRepository.findById(id).orElseThrow(() -> {
            throw new IllegalArgumentException("없는 id");
        });
    }
}
