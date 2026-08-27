package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.auth.*;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.exception.InvalidCredentialsException;
import com.nhnacademy.insightonauth.exception.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.RefreshTokenNotFoundException;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private static final String X_USER_ID = "X-User-Id";

    private final UserAuthenticationService userAuthenticationService;
    private final UserEmailService userEmailService;
    private final UserManagementService userManagementService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProvider jwtProvider;

    @PostMapping("/email/verify-request")
    public ResponseEntity<Void> sendEmailVerify(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userEmailService.emailVerifyRequest(emailVerifyRequest.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify-confirm")
    public ResponseEntity<EmailVerifyConfirmResponse> emailCodeConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        String verificationToken =
                userEmailService.emailVerifyConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(new EmailVerifyConfirmResponse(verificationToken));
    }

    @PostMapping("/check-email")
    public ResponseEntity<EmailAvailableResponse> checkEmailAvailable(
            @RequestBody @Valid EmailAvailableRequest emailAvailableRequest) {
        boolean available = userManagementService.checkEmailAvailable(emailAvailableRequest.email());

        return ResponseEntity.ok(new EmailAvailableResponse(available));
    }

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

    @PostMapping("/login")
    public ResponseEntity<String> doLogin(
            @RequestBody @Valid UserLoginRequest userLoginRequest) {
        UserLoginResponse userLoginResponse = userAuthenticationService.login(userLoginRequest.email(), userLoginRequest.password());

        @SuppressWarnings("unchecked")
        List<String> roles =
                (List<String>) jwtProvider.parse(userLoginResponse.accessToken()).get("roles");
        if (roles != null && roles.contains("ADMIN")) {
            // 관리자라고 알려주지 않고, 그냥 일반 로그인 실패처럼 처리
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 도커용
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", userLoginResponse.refreshToken())
                .httpOnly(true)
                .secure(true)           // https라 true
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build();               // domain 안 박음

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())   // refresh 쿠키 헤더에 설정
                .body(userLoginResponse.accessToken());

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> doLogout(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        userAuthenticationService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactive")
    public ResponseEntity<UserLoginResponse> userReactive(
            @RequestBody @Valid ReactiveRequest request) {

        UserLoginResponse response = userEmailService.reactive(request.reactiveToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reactivate/email-verify-request")
    public ResponseEntity<Void> userReactivateRequest(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userEmailService.reactivateRequest(emailVerifyRequest.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactivate/email-verify-confirm")
    public ResponseEntity<UserLoginResponse> userReactiveConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        UserLoginResponse userLoginResponse =
                userEmailService.reactivateConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(userLoginResponse);
    }

    @PostMapping("/find-email")
    public ResponseEntity<String> findEmail(@RequestBody @Valid FindEmailRequest findEmailRequest) {
        String email = userManagementService.findMaskedEmail(findEmailRequest.userName(), findEmailRequest.phoneNumber());

        return ResponseEntity.ok(email);
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> passwordReset(@RequestBody @Valid PasswordResetRequest passwordResetRequest) {
        userEmailService.passwordResetRequest(passwordResetRequest.email());
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 재설시 전과 동일한비밀번호는 막게 수정
    @PostMapping("/password/reset-confirm")
    public ResponseEntity<Void> passwordResetConfirm(
            @RequestBody @Valid PasswordResetConfirmRequest passwordResetConfirmRequest) {
        userEmailService.passwordResetConfirm(passwordResetConfirmRequest.token(), passwordResetConfirmRequest.password());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/oauth/{provider}")
    public ResponseEntity<UserLoginResponse> oauthLogin(
            @PathVariable String provider,
            @RequestBody @Valid OauthLoginRequest request) {

        UserLoginResponse response = userAuthenticationService.oauthLogin(provider, request.code());
        return ResponseEntity.ok(response);
    }

    // (refreshToken에서 userId 추출 — 방식은 createRefreshToken에 따라)
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

    @GetMapping("/tokens/{jti}/blacklisted")
    public ResponseEntity<Boolean> blacklistedCheck(
            @PathVariable("jti") @NotBlank String jti) {
        boolean blacklisted = tokenBlacklistService.isBlacklisted(jti);
        return ResponseEntity.ok(blacklisted);
    }
}
