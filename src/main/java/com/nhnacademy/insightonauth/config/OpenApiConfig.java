package com.nhnacademy.insightonauth.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InsightOn Auth API")
                        .description("회원가입, 로그인/로그아웃, 소셜 로그인, 마이페이지, 관리자 회원 관리 API")
                        .version("v1.0.0."))
                .addServersItem(new Server().url("https://insighton.store").description("배포 환경 (Gateway 경유)"))
                .addServersItem(new Server().url("http://localhost:8000").description("로컬 개발 환경"));
    }
}
