package com.nhnacademy.insightonauth.controller.swagger;

import com.nhnacademy.insightonauth.dto.auth.EmailAvailableRequest;
import com.nhnacademy.insightonauth.dto.auth.EmailAvailableResponse;
import com.nhnacademy.insightonauth.dto.auth.EmailVerifyConfirmRequest;
import com.nhnacademy.insightonauth.dto.auth.EmailVerifyConfirmResponse;
import com.nhnacademy.insightonauth.dto.auth.EmailVerifyRequest;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginRequest;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserSignupRequest;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code AuthController})에 있다.
 */
@Tag(name = "Auth", description = "회원가입, 로그인/로그아웃, 소셜 로그인, 액세스 토큰 재발급")
public interface AuthApi {

    @Operation(summary = "이메일 인증코드 발송", description = "회원가입용 이메일 인증 코드를 발송한다. 재전송 쿨다운·횟수 제한이 적용된다.")
    @ApiResponse(responseCode = "204", description = "발송 성공")
    ResponseEntity<Void> sendEmailVerify(EmailVerifyRequest emailVerifyRequest);

    @Operation(summary = "이메일 인증코드 확인", description = "인증 코드 확인 성공 시 가입 요청에 쓸 인증 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "확인 성공, 인증 토큰 반환")
    ResponseEntity<EmailVerifyConfirmResponse> emailCodeConfirm(EmailVerifyConfirmRequest emailVerifyConfirmRequest);

    @Operation(summary = "이메일 중복 확인", description = "가입 폼 실시간 검사용으로 이메일 사용 가능 여부를 확인한다.")
    @ApiResponse(responseCode = "200", description = "확인 성공")
    ResponseEntity<EmailAvailableResponse> checkEmailAvailable(EmailAvailableRequest emailAvailableRequest);

    @Operation(summary = "회원가입", description = "이메일 인증 확인에서 받은 인증 토큰이 필요하다. 역할은 MEMBER로 고정된다.")
    @ApiResponse(responseCode = "201", description = "가입 성공")
    ResponseEntity<UserSignupResponse> doSignup(UserSignupRequest userSignupRequest);

    @Operation(summary = "일반 회원 로그인",
            description = "access 토큰은 응답 본문, refresh 토큰은 HttpOnly 쿠키로 내려간다. 관리자 계정은 이 경로로 로그인할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 또는 탈퇴 후 복구 대기(PENDING_RESTORE) 안내"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치(관리자 계정 포함)", content = @Content)
    })
    ResponseEntity<UserLoginResponse> doLogin(UserLoginRequest userLoginRequest);

    @Operation(summary = "로그아웃", description = "refresh 토큰을 삭제하고 현재 access 토큰을 블랙리스트에 등록한다.")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> doLogout(
            @Parameter(description = "요청자 사용자 ID (게이트웨이 주입)", required = true) Long userId,
            @Parameter(description = "Authorization 헤더 (Bearer {accessToken})", required = true) String token);

    @Operation(summary = "소셜 로그인 (SPA 방식)",
            description = "provider(google, github ...) 코드로 로그인한다. 연동 계정이 없으면 신규 가입 처리된다. "
                    + "응답 규약은 일반 로그인과 동일(access는 바디, refresh는 쿠키).")
    @ApiResponse(responseCode = "200", description = "로그인 성공 또는 탈퇴 후 복구 대기(PENDING_RESTORE) 안내")
    ResponseEntity<UserLoginResponse> oauthLogin(
            @Parameter(description = "OAuth provider", example = "google", required = true) String provider,
            OauthLoginRequest request);

    @Operation(summary = "브라우저 주도 소셜 로그인 시작",
            description = "프론트 버튼 클릭 → 여기로 리다이렉트 → provider 동의 화면으로 302. "
                    + "state 값을 담은 oauthState 쿠키를 심는다.")
    @ApiResponse(responseCode = "302", description = "provider 동의 화면으로 리다이렉트(실패 시 로그인 에러 페이지로 리다이렉트)")
    ResponseEntity<Void> oauthAuthorize(
            @Parameter(description = "OAuth provider", example = "google", required = true) String provider);

    @Operation(summary = "마이페이지 소셜 계정 연동 시작",
            description = "마이페이지 '연동' 버튼 → 여기 → provider 동의 화면 → GET /oauth/callback 으로 이어지는 "
                    + "브라우저 주도 왕복. mergeWith가 있으면 병합 확인 후 콜백에서 연동 대신 병합을 수행한다.")
    @ApiResponse(responseCode = "302", description = "provider 동의 화면으로 리다이렉트(인증 실패 시 마이페이지 에러 리다이렉트)")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> oauthLinkAuthorize(
            @Parameter(description = "OAuth provider", example = "google", required = true) String provider,
            @Parameter(description = "이미 다른 계정에 연동된 소셜 계정을 병합할 대상 userId") Long mergeWith,
            @Parameter(description = "accessToken 쿠키(연동을 시작한 로그인 유저 식별용)") String accessToken);

    @Operation(summary = "소셜 로그인/연동 콜백",
            description = "provider가 인가 코드와 함께 되돌아오는 엔드포인트. 서버가 심어둔 oauthState 쿠키와 대조해 "
                    + "로그인/연동 여부를 판단하고, 성공 시 프론트 페이지로 302 리다이렉트한다. 사용자가 직접 호출하는 API가 아니다.")
    @ApiResponse(responseCode = "302", description = "결과에 따라 로그인 완료/재활성화 안내/마이페이지/에러 페이지로 리다이렉트")
    ResponseEntity<Void> oauthCallback(
            @Parameter(description = "provider 인가 코드") String code,
            @Parameter(description = "요청 시 심었던 state 값(쿼리)") String state,
            @Parameter(description = "provider가 반환한 오류 코드(취소 등)") String error,
            @Parameter(description = "서버가 심은 oauthState 쿠키 값(검증 기준)") String expectedState,
            @Parameter(description = "accessToken 쿠키(연동 콜백일 때 현재 로그인 유저 식별용)") String accessToken);

    @Operation(summary = "액세스 토큰 재발급", description = "refreshToken 쿠키로 새 access 토큰을 재발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "refreshToken 쿠키 없음 또는 서명·만료 검증 실패")
    })
    ResponseEntity<TokenRefreshResponse> refresh(
            @Parameter(description = "refreshToken 쿠키", required = true) String refreshToken);
}
