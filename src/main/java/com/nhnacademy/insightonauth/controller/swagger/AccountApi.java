package com.nhnacademy.insightonauth.controller.swagger;

import com.nhnacademy.insightonauth.dto.auth.EmailVerifyConfirmRequest;
import com.nhnacademy.insightonauth.dto.auth.EmailVerifyRequest;
import com.nhnacademy.insightonauth.dto.auth.FindEmailRequest;
import com.nhnacademy.insightonauth.dto.auth.PasswordResetConfirmRequest;
import com.nhnacademy.insightonauth.dto.auth.PasswordResetRequest;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * 문서 전용 인터페이스. 실제 매핑/바인딩 어노테이션은 구현체({@code AccountController})에 있다.
 * 계정 접근을 잃어버린 사용자의 복구 흐름(이메일/비밀번호 찾기, 탈퇴 계정 재활성화)을 다룬다.
 */
@Tag(name = "Account", description = "이메일/비밀번호 찾기, 탈퇴 계정 재활성화")
public interface AccountApi {

    @Operation(summary = "재활성화 이메일 인증코드 발송",
            description = "탈퇴 후 복구 가능 기간 내 계정을 재활성화하기 위한 인증 코드를 발송한다.")
    @ApiResponse(responseCode = "204", description = "발송 성공")
    ResponseEntity<Void> userReactivateRequest(EmailVerifyRequest emailVerifyRequest);

    @Operation(summary = "재활성화 이메일 인증코드 확인",
            description = "인증 코드 확인 성공 시 탈퇴 계정을 ACTIVE로 복구하고 로그인 처리한다. "
                    + "응답 규약은 일반 로그인과 동일(access는 바디, refresh는 쿠키).")
    @ApiResponse(responseCode = "200", description = "재활성화 및 로그인 성공")
    ResponseEntity<UserLoginResponse> userReactiveConfirm(EmailVerifyConfirmRequest emailVerifyConfirmRequest);

    @Operation(summary = "가입 이메일 찾기", description = "이름 + 전화번호로 가입 이메일을 찾는다. 앞 2글자만 남기고 마스킹해서 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공(마스킹된 이메일)")
    ResponseEntity<String> findEmail(FindEmailRequest findEmailRequest);

    @Operation(summary = "비밀번호 재설정 메일 발송", description = "재설정 링크 메일 발송을 요청한다. 계정 존재 여부는 응답으로 드러내지 않는다.")
    @ApiResponse(responseCode = "204", description = "요청 접수(계정 존재 여부와 무관하게 동일 응답)")
    ResponseEntity<Void> passwordReset(PasswordResetRequest passwordResetRequest);

    @Operation(summary = "비밀번호 재설정 확인", description = "메일 링크의 토큰으로 새 비밀번호를 설정한다. 기존과 동일한 비밀번호는 서비스에서 차단한다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    ResponseEntity<Void> passwordResetConfirm(PasswordResetConfirmRequest passwordResetConfirmRequest);
}
