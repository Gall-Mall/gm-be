package com.gm.db.domain.member;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.gm.core.domain.member.model.Member;
import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor
public class MemberEntity extends BaseEntity {

    private String name;
    private String phoneNumber;

    public MemberEntity(String name, String phoneNumber){
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public Member toDomainModel(){
        return new Member(name, phoneNumber);
    }
}
