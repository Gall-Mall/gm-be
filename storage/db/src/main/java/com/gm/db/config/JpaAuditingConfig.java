package com.gm.db.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@code @CreatedDate}와 {@code @LastModifiedDate} 기반 JPA 감사를 활성화한다.
 *
 * <p>실행 애플리케이션이 명시적으로 가져와 저장 모듈의 감사 설정을 적용한다.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
