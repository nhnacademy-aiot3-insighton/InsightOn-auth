package com.nhnacademy.insightonauth.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code TokenController})에 있다.
 * 사용자 요청이 아니라 게이트웨이가 요청마다 호출하는 service-to-service 엔드포인트다.
 */
@Tag(name = "Token", description = "게이트웨이용 토큰 블랙리스트 조회")
public interface TokenApi {

    @Operation(summary = "액세스 토큰 블랙리스트 여부 조회",
            description = "주어진 jti(액세스 토큰 ID)가 블랙리스트에 있는지 반환한다. 게이트웨이가 인가 전에 호출한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공(블랙리스트 여부 boolean)")
    ResponseEntity<Boolean> blacklistedCheck(
            @Parameter(description = "액세스 토큰의 jti", required = true) @NotBlank String jti);
}
