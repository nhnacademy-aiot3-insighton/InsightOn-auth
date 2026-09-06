package com.nhnacademy.insightonauth.controller.swagger;

import com.nhnacademy.insightonauth.dto.core.AuthUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code CoreController})에 있다.
 * 다른 내부 서비스(core 등)가 호출하는 service-to-service 엔드포인트다.
 */
@Tag(name = "Core", description = "내부 서비스용 사용자 조회")
public interface CoreApi {

    @Operation(summary = "userId로 사용자 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<AuthUserResponse> getUserById(
            @Parameter(description = "사용자 ID", required = true) Long userId);

    @Operation(summary = "이메일로 사용자 조회", description = "초대 등 이메일 기반 조회용.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<AuthUserResponse> getUserByEmail(
            @Parameter(description = "사용자 이메일", required = true) String userEmail);
}
