package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.auth.*;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.exception.auth.InvalidCredentialsException;
import com.nhnacademy.insightonauth.exception.auth.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.auth.RefreshTokenNotFoundException;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 정상 인증 흐름을 담당하는 컨트롤러.
 * 회원가입(이메일 인증 포함), 로그인/로그아웃, 소셜 로그인, 액세스 토큰 재발급을 처리한다.
 * 계정을 잃어버린 뒤의 복구 흐름은 {@link AccountController}가 담당한다.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String X_USER_ID = "X-User-Id";

    private final UserAuthenticationService userAuthenticationService;
    private final UserEmailService userEmailService;
    private final UserManagementService userManagementService;
    private final JwtProvider jwtProvider;
    private final LoginResponder loginResponder;

    // 회원가입용 이메일 인증 코드 발송 요청 (재전송 쿨다운·횟수 제한 적용)
    @PostMapping("/email/verify-request")
    public ResponseEntity<Void> sendEmailVerify(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userEmailService.emailVerifyRequest(emailVerifyRequest.email());
        return ResponseEntity.noContent().build();
    }

    // 이메일 인증 코드 확인 → 성공 시 가입 요청에 쓸 인증 토큰 발급
    @PostMapping("/email/verify-confirm")
    public ResponseEntity<EmailVerifyConfirmResponse> emailCodeConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        String verificationToken =
                userEmailService.emailVerifyConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(new EmailVerifyConfirmResponse(verificationToken));
    }

    // 이메일 중복 여부 확인 (가입 폼 실시간 검사용)
    @PostMapping("/check-email")
    public ResponseEntity<EmailAvailableResponse> checkEmailAvailable(
            @RequestBody @Valid EmailAvailableRequest emailAvailableRequest) {
        boolean available = userManagementService.checkEmailAvailable(emailAvailableRequest.email());

        return ResponseEntity.ok(new EmailAvailableResponse(available));
    }

    // 회원가입 (verify-confirm에서 받은 인증 토큰 필요, 역할은 MEMBER 고정)
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> doSignup(
            @RequestBody @Valid UserSignupRequest userSignupRequest) {
        UserSignupResponse userSignupResponse =
                userManagementService.createUser(userSignupRequest.email(),
                        userSignupRequest.password(),
                        userSignupRequest.userName(),
                        userSignupRequest.phoneNumber(),
                        Role.MEMBER,
                        userSignupRequest.token());

        return ResponseEntity.status(HttpStatus.CREATED).body(userSignupResponse);
    }

    // 일반 회원 로그인 — access 토큰은 본문, refresh 토큰은 HttpOnly 쿠키. 관리자 계정은 이 경로로 로그인 불가
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> doLogin(
            @RequestBody @Valid UserLoginRequest userLoginRequest) {
        UserLoginResult result = userAuthenticationService.login(
                userLoginRequest.email(), userLoginRequest.password());

        // 탈퇴 후 복구 가능 기간 내 계정 — 로그인 성공이 아니라 "복구 안내" 상태.
        // accessToken 이 없으므로 admin 체크 대상이 아니다.
        if ("PENDING_RESTORE".equals(result.status())) {
            return ResponseEntity.ok(UserLoginResponse.pendingRestore());
        }

        if (jwtProvider.hasAdminRole(result.accessToken())) {
            // 계정 열거 방지: 관리자 / 비번 오류 / 없는 계정 모두 동일 메시지
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return loginResponder.success(result);
    }

    // 로그아웃 — refresh 토큰 삭제 + 현재 access 토큰 블랙리스트 등록
    @PostMapping("/logout")
    public ResponseEntity<Void> doLogout(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        userAuthenticationService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    // 소셜 로그인 (provider: google, github ...) — 연동 계정이 없으면 신규 가입 처리.
    // 응답 규약은 일반 로그인과 동일 (access 토큰은 바디, refresh 토큰은 HttpOnly 쿠키).
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<UserLoginResponse> oauthLogin(
            @PathVariable String provider,
            @RequestBody @Valid OauthLoginRequest request) {

        UserLoginResult result = userAuthenticationService.oauthLogin(provider, request.code());

        // 탈퇴 후 복구 가능 기간 내 계정 — 로그인 성공이 아니라 "복구 안내" 상태.
        if ("PENDING_RESTORE".equals(result.status())) {
            return ResponseEntity.ok(UserLoginResponse.pendingRestore());
        }

        return loginResponder.success(result);
    }

    // refresh 토큰 쿠키로 새 access 토큰 재발급 (쿠키 없음/서명·만료 오류면 예외)
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue("refreshToken") String refreshToken) {
        // 1) refreshToken 쿠키 없음
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenNotFoundException("refreshToken이 없습니다.");
        }

        // 2) 파싱(서명·만료 검증) 실패
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.parse(refreshToken).getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new InvalidRefreshTokenException("유효하지 않은 refreshToken입니다.");
        }

        // 3) refresh 처리
        TokenRefreshResponse tokenRefreshResponse =
                userAuthenticationService.refresh(userId, refreshToken);
        return ResponseEntity.ok(tokenRefreshResponse);
    }
}
