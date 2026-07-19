package com.gm.architecture;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 모듈 간 의존 방향을 강제하는 아키텍처 규칙.
 *
 * <p>api 모듈은 core/storage/client/mq 를 모두 조립하므로, 테스트 클래스패스에서
 * 전체 {@code com.gm} 패키지를 관찰할 수 있어 여기서 규칙을 검증한다.</p>
 */
@AnalyzeClasses(packages = "com.gm")
class ArchUnitTest {

    /**
     * core 는 순수 도메인 모듈이다.
     * 다른 어떤 애플리케이션 모듈(api/storage/client/mq)도 참조해서는 안 된다.
     */
    @ArchTest
    static final ArchRule core_는_다른_모듈을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.core..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.api..",
                            "com.gm.db..",
                            "com.gm.redis..",
                            "com.gm.client..",
                            "com.gm.mq..")
                    .as("core 모듈은 다른 모듈을 참조할 수 없다");

    /**
     * api 는 storage(db/redis)·mq 를 직접 참조할 수 없다.
     *
     * <p>애플리케이션 진입점(@SpringBootApplication 이 붙은 조립 지점)만
     * 컴포넌트 스캔·빈 구성을 위해 어댑터 구성을 참조하는 것을 허용한다.
     * 컨트롤러·서비스 등 나머지 코드는 어댑터를 직접 참조할 수 없다.</p>
     */
    @ArchTest
    static final ArchRule api_는_어댑터를_직접_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.api..")
                    .and().areNotAnnotatedWith(SpringBootApplication.class)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.db..",
                            "com.gm.redis..",
                            "com.gm.mq..")
                    .as("api 모듈은 storage(db/redis)·mq 를 직접 참조할 수 없다 (조립 지점 제외)");

    /**
     * storage:db 는 말단 어댑터다.
     * api·client·mq 나 다른 storage(redis) 를 참조해서는 안 된다. (db → db 는 허용)
     */
    @ArchTest
    static final ArchRule db는_다른_모듈을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.db..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.api..",
                            "com.gm.client..",
                            "com.gm.redis..",
                            "com.gm.mq..")
                    .as("storage:db 모듈은 api/client/redis/mq 를 참조할 수 없다");

    /**
     * storage:redis 는 말단 어댑터다.
     * api·client·mq 나 다른 storage(db) 를 참조해서는 안 된다. (redis → redis 는 허용)
     */
    @ArchTest
    static final ArchRule redis는_다른_모듈을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.redis..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.api..",
                            "com.gm.client..",
                            "com.gm.db..",
                            "com.gm.mq..")
                    // redis 모듈은 아직 소스가 없을 수 있어 빈 대상 허용
                    .allowEmptyShould(true)
                    .as("storage:redis 모듈은 api/client/db/mq 를 참조할 수 없다");

    /**
     * client 는 외부 연동 말단 어댑터다.
     * api 나 storage(db/redis)·mq 를 참조해서는 안 된다.
     */
    @ArchTest
    static final ArchRule client_는_다른_모듈을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.client..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.api..",
                            "com.gm.db..",
                            "com.gm.redis..",
                            "com.gm.mq..")
                    // client 모듈은 아직 소스가 없을 수 있어 빈 대상 허용
                    .allowEmptyShould(true)
                    .as("client 모듈은 api/storage/mq 를 참조할 수 없다");

    /**
     * mq 는 메시징 말단 어댑터다.
     * api 나 storage(db/redis)·client 를 참조해서는 안 된다. (mq → core 는 허용)
     */
    @ArchTest
    static final ArchRule mq는_다른_모듈을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.gm.mq..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.gm.api..",
                            "com.gm.db..",
                            "com.gm.redis..",
                            "com.gm.client..")
                    .as("mq 모듈은 api/storage/client 를 참조할 수 없다");

    /**
     * 모듈(최상위 com.gm.* 슬라이스) 사이에 순환 참조가 없어야 한다.
     */
    @ArchTest
    static final ArchRule 모듈간_순환_참조가_없다 =
            slices().matching("com.gm.(*)..").should().beFreeOfCycles()
                    .as("com.gm 하위 모듈 사이에 순환 참조가 없어야 한다");
}
