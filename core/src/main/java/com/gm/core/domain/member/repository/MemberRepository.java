package com.gm.core.domain.member.repository;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.member.model.Member;

public interface MemberRepository {
    Optional<Member> findById(UUID id);
}
