package com.nhnacademy.insightonauth.controller.swagger;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.admin.RoleResponse;
import com.nhnacademy.insightonauth.dto.admin.RolesUpdateRequest;
import com.nhnacademy.insightonauth.dto.auth.UserLoginRequest;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code AdminController})에 있다.
 * 로그인을 제외한 모든 엔드포인트는 ADMIN 권한이 필요하다.
 */
@Tag(name = "Admin", description = "관리자 로그인, 회원 조회/권한 관리")
public interface AdminApi {

    @Operation(summary = "관리자 로그인",
            description = "응답 규약은 일반 로그인과 동일. 비관리자 계정은 이 경로로 로그인할 수 없다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 또는 탈퇴 후 복구 대기(PENDING_RESTORE) 안내")
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치(비관리자 계정 포함)")
    ResponseEntity<UserLoginResponse> doLogin(UserLoginRequest userLoginRequest);

    @Operation(summary = "회원 목록 조회", description = "검색·페이징으로 회원 목록을 조회한다. status가 빈값/생략이면 전체 상태를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "알 수 없는 status 값")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<PageResponse<AdminFindUsersResponse>> findUsers(
            @Parameter(description = "이메일 검색어") String email,
            @Parameter(description = "이름 검색어") String userName,
            @Parameter(description = "상태 필터(ACTIVE, SLEEP, BLOCKED, WITHDRAWN 등), 생략 시 전체") String status,
            Pageable pageable);

    @Operation(summary = "회원 상세 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<AdminUserDetailResponse> findUserDetail(
            @Parameter(description = "회원 ID", required = true) Long userId);

    @Operation(summary = "지정 가능한 권한 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<List<RoleResponse>> roles();

    @Operation(summary = "회원 계정 차단")
    @ApiResponse(responseCode = "204", description = "차단 성공")
    @ApiResponse(responseCode = "403", description = "자기 자신을 대상으로 지정함")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> block(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
                    description = "요청자 관리자 ID (게이트웨이 주입)", required = true) Long adminId,
            @Parameter(description = "회원 ID", required = true) Long userId);

    @Operation(summary = "회원 계정 휴면 전환")
    @ApiResponse(responseCode = "204", description = "전환 성공")
    @ApiResponse(responseCode = "403", description = "자기 자신을 대상으로 지정함")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> sleep(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
                    description = "요청자 관리자 ID (게이트웨이 주입)", required = true) Long adminId,
            @Parameter(description = "회원 ID", required = true) Long userId);

    @Operation(summary = "회원 계정 활성화(복구)")
    @ApiResponse(responseCode = "204", description = "활성화 성공")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> activate(@Parameter(description = "회원 ID", required = true) Long userId);

    @Operation(summary = "회원 권한 변경", description = "요청 바디의 목록으로 권한을 전체 교체한다(유지/추가/삭제 자동 계산).")
    @ApiResponse(responseCode = "204", description = "변경 성공")
    @ApiResponse(responseCode = "403", description = "자기 자신을 대상으로 지정함")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> updateRoles(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
                    description = "요청자 관리자 ID (게이트웨이 주입)", required = true) Long adminId,
            @Parameter(description = "회원 ID", required = true) Long userId,
            RolesUpdateRequest request);

    @Operation(summary = "회원 강제 로그아웃")
    @ApiResponse(responseCode = "204", description = "강제 로그아웃 성공")
    @ApiResponse(responseCode = "403", description = "자기 자신을 대상으로 지정함")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> forceLogout(
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER,
                    description = "요청자 관리자 ID (게이트웨이 주입)", required = true) Long adminId,
            @Parameter(description = "회원 ID", required = true) Long userId);
}
