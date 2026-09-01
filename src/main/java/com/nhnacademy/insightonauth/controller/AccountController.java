package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.auth.*;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 계정 접근을 잃어버린 사용자의 복구 흐름을 담당하는 컨트롤러.
 * 이메일(아이디) 찾기, 비밀번호 재설정, 탈퇴/휴면 계정 재활성화를 처리한다.
 * 정상적으로 로그인하는 흐름은 {@link AuthController}가 담당한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountController {

    private final UserEmailService userEmailService;
    private final UserManagementService userManagementService;
    private final LoginResponder loginResponder;

    // 재활성화용 이메일 인증 코드 발송 (사용자가 직접 로그인 화면에서 복구를 시작하는 경로)
    @PostMapping("/reactivate/email-verify-request")
    public ResponseEntity<Void> userReactivateRequest(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userEmailService.reactivateRequest(emailVerifyRequest.email());
        return ResponseEntity.noContent().build();
    }

    // 인증 코드 확인 후 탈퇴 계정을 ACTIVE로 복구 + 로그인
    // 응답 규약은 일반 로그인과 동일 (access 토큰은 바디, refresh 토큰은 HttpOnly 쿠키).
    @PostMapping("/reactivate/email-verify-confirm")
    public ResponseEntity<UserLoginResponse> userReactiveConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        UserLoginResult result =
                userEmailService.reactivateConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return loginResponder.success(result);
    }

    // 이름 + 전화번호로 가입 이메일 찾기 — 앞 2글자만 남기고 마스킹해서 반환
    @PostMapping("/find-email")
    public ResponseEntity<String> findEmail(@RequestBody @Valid FindEmailRequest findEmailRequest) {
        String email = userManagementService.findMaskedEmail(findEmailRequest.userName(), findEmailRequest.phoneNumber());

        return ResponseEntity.ok(email);
    }

    // 비밀번호 재설정 링크 메일 발송 요청 — 계정 존재 여부는 응답으로 드러내지 않음
    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> passwordReset(@RequestBody @Valid PasswordResetRequest passwordResetRequest) {
        userEmailService.passwordResetRequest(passwordResetRequest.email());
        return ResponseEntity.noContent().build();
    }

    // 메일 링크의 토큰으로 새 비밀번호 설정 (기존과 동일한 비밀번호는 서비스에서 차단)
    @PostMapping("/password/reset-confirm")
    public ResponseEntity<Void> passwordResetConfirm(
            @RequestBody @Valid PasswordResetConfirmRequest passwordResetConfirmRequest) {
        userEmailService.passwordResetConfirm(passwordResetConfirmRequest.token(), passwordResetConfirmRequest.password());

        return ResponseEntity.ok().build();
    }
}
