package com.gm.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * 갈래말래 API 문서에 노출할 기본 OpenAPI 정보를 구성한다.
 *
 * <p>인증 방식과 서버 URL은 아직 확정되지 않았으므로 현재 설정에 포함하지 않는다.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 서비스명·설명·API 버전이 포함된 OpenAPI 모델을 생성한다.
     *
     * @return Swagger 문서 생성에 사용할 OpenAPI 모델
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("갈래말래 API")
                        .description("국내 위치 기반 그룹 메뉴·식당 의사결정 서비스 API")
                        .version("v1"));
    }
}
