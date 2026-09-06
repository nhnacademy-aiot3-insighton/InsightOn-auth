package com.nhnacademy.insightonauth.controller.swagger;

import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoUpdateRequest;
import com.nhnacademy.insightonauth.dto.mypage.MyRoleResponse;
import com.nhnacademy.insightonauth.dto.mypage.PasswordChangeRequest;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code MypageController})에 있다.
 * 모든 엔드포인트는 로그인한 본인만 접근 가능하다(userId는 게이트웨이가 X-User-Id 헤더로 주입).
 */
@Tag(name = "Mypage", description = "내 정보 조회/수정, 비밀번호 변경, 소셜 계정 연동 관리, 탈퇴")
@SecurityRequirement(name = "bearerAuth")
public interface MypageApi {

    @Operation(summary = "내 정보 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<MyInfoResponse> findMyInfo(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId);

    @Operation(summary = "내 정보 수정", description = "이름과 전화번호를 수정한다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    ResponseEntity<Void> updateMyInfo(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId,
            MyInfoUpdateRequest request);

    @Operation(summary = "회원 탈퇴")
    @ApiResponse(responseCode = "204", description = "탈퇴 성공")
    ResponseEntity<Void> withdraw(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId,
            @Parameter(description = "Authorization 헤더 (Bearer {accessToken})", required = true) String token);

    @Operation(summary = "비밀번호 변경")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    ResponseEntity<Void> changePassword(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId,
            PasswordChangeRequest request);

    @Operation(summary = "내 권한 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<List<MyRoleResponse>> findMyRoles(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId);

    @Operation(summary = "연동된 소셜 계정 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<List<OauthResponse>> findMyOauths(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId);

    @Operation(summary = "소셜 계정 연동 해제",
            description = "신규 연동은 브라우저 주도 왕복이 필요해 auth의 "
                    + "GET /api/v1/auth/oauth/link/authorize/{provider} 가 담당한다.")
    @ApiResponse(responseCode = "204", description = "해제 성공")
    ResponseEntity<Void> unlinkOauth(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId,
            @Parameter(description = "연동 해제할 OAuth 계정 ID", required = true) Long oauthId);
}
