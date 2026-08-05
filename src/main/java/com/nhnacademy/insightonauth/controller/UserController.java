package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.*;
import com.nhnacademy.insightonauth.dto.auth.*;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private static final String X_USER_ID = "X-User-Id";

    private final UserService userService;

    @PostMapping("/email/verify-request")
    public ResponseEntity<Void> sendEmailVerify(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userService.emailVerifyRequest(emailVerifyRequest.email());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify-confirm")
    public ResponseEntity<ApiResponse<EmailVerifyConfirmResponse>> emailCodeConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        String verificationToken =
                userService.emailVerifyConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(new ApiResponse<>(new EmailVerifyConfirmResponse(verificationToken)));
    }

    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailAvailableResponse>> checkEmailAvailable(
            @RequestBody @Valid EmailAvailableRequest emailAvailableRequest) {
        boolean available = userService.checkEmailAvailable(emailAvailableRequest.email());

        return ResponseEntity.ok(new ApiResponse<>(new EmailAvailableResponse(available)));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignupResponse>> doSignup(
            @RequestBody @Valid UserSignupRequest userSignupRequest) {
        UserSignupResponse userSignupResponse =
                userService.createUser(userSignupRequest.email(),
                    userSignupRequest.password(),
                    userSignupRequest.userName(),
                    userSignupRequest.phoneNumber(),
                    Role.MEMBER,
                    userSignupRequest.token());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(userSignupResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> doLogin(
            @RequestBody @Valid UserLoginRequest userLoginRequest) {
        UserLoginResponse userLoginResponse = userService.login(userLoginRequest.email(), userLoginRequest.password());

        return ResponseEntity.ok(new ApiResponse<>(userLoginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> doLogout(@RequestHeader(name = X_USER_ID) @Valid Long userId) {
        userService.logout(userId);

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/reactive")
    public ResponseEntity<ApiResponse<UserLoginResponse>> userReactive(
            @RequestBody @Valid ReactiveRequest request) {

        UserLoginResponse response = userService.reactive(request.reactiveToken());
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/reactivate/email-verify-request")
    public ResponseEntity<Void> userReactive(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userService.reactivateRequest(emailVerifyRequest.email());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactivate/email-verify-confirm")
    public ResponseEntity<ApiResponse<UserLoginResponse>> userReactiveConfirm(@RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        UserLoginResponse userLoginResponse = userService.reactivateConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(new ApiResponse<>(userLoginResponse));
    }

    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(@RequestBody @Valid FindEmailRequest findEmailRequest) {
        String email = userService.findMaskedEmail(findEmailRequest.userName(), findEmailRequest.phoneNumber());

        return ResponseEntity.ok(new ApiResponse<>(email));
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<Void>> passwordReset(@RequestBody @Valid PasswordResetRequest passwordResetRequest) {
        userService.passwordResetRequest(passwordResetRequest.email());

        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/password/reset-confirm")
    public ResponseEntity<Void> passwordResetConfirm(@RequestBody @Valid PasswordResetConfirmRequest passwordResetConfirmRequest) {
        userService.passwordResetConfirm(passwordResetConfirmRequest.token(), passwordResetConfirmRequest.password());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/oauth/{provider}")
    public ResponseEntity<ApiResponse<UserLoginResponse>> oauthLogin(
            @PathVariable String provider,
            @RequestBody @Valid OauthLoginRequest request) {

        UserLoginResponse response = userService.oauthLogin(provider, request.code());
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @CookieValue("refreshToken") String refreshToken) {
        TokenRefreshResponse tokenRefreshResponse = userService.refresh(userId, refreshToken);

        return ResponseEntity.ok(new ApiResponse<>(tokenRefreshResponse));
    }
}
