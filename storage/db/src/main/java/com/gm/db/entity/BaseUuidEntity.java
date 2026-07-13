package com.gm.db.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UuidGenerator;

/**
 * UUID 식별자를 사용하는 JPA 엔티티의 공통 기반 클래스이다.
 *
 * <p>식별자는 영속화 시 Hibernate가 시간 순서 특성을 가진 UUIDv7으로 생성한다.
 * 실제 DB 컬럼 형식은 특정 데이터베이스가 확정될 때 별도로 결정한다.</p>
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseUuidEntity extends BaseEntity {

    /** 영속화 시 생성되며 이후 변경되지 않는 엔티티 식별자이다. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

}
