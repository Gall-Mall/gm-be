package com.gm.db.common.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 JPA 엔티티가 공유하는 생성·수정 시각 감사 기반 클래스이다.
 *
 * <p>감사 시각은 서버의 지역 시간과 분리된 절대 시점인 {@link Instant}로 보관한다.
 * 식별자와 삭제 정책은 엔티티마다 다를 수 있으므로 이 클래스에 포함하지 않는다.</p>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    /** 영속화 시 생성되며 이후 변경되지 않는 엔티티 식별자이다. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** 엔티티가 최초로 영속화된 시각이다. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 엔티티가 마지막으로 변경된 시각이다. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
