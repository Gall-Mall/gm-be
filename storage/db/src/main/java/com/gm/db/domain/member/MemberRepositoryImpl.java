package com.gm.db.domain.member;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.member.model.Member;
import com.gm.core.domain.member.repository.MemberRepository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    public Optional<Member> findById(UUID id){
        return memberJpaRepository.findById(id).map(MemberEntity::toDomainModel);
    }
}
