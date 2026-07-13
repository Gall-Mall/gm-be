package com.gm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.gm.db.config.JpaAuditingConfig;

/**
 * 갈래말래 HTTP API를 실행하고 각 인프라 모듈의 구성을 조립하는 진입점이다.
 *
 * <p>DB 엔티티·Repository 검색 범위를 명시하고 JPA 감사 설정을 가져온다.</p>
 */
@SpringBootApplication(scanBasePackages = "com.gm")
@EntityScan(basePackages = "com.gm.db")
@EnableJpaRepositories(basePackages = "com.gm.db")
@Import(JpaAuditingConfig.class)
public class ApiApplication {

    /**
     * Spring Boot API 애플리케이션을 시작한다.
     *
     * @param args 실행 시 전달된 명령행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}
