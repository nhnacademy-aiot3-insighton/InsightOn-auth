package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.PasswordChangeRequest;
import com.nhnacademy.insightonauth.dto.mypage.MyRoleResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoUpdateRequest;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.service.MyPageService;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MypageController {

    private static final String X_USER_ID = "X-User-Id";

    private final OauthService oauthService;
    private final MyPageService myPageService;
    private final UserManagementService userManagementService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> findMyInfo(
            @RequestHeader(name = X_USER_ID) Long userId) {

        MyInfoResponse response = myPageService.findMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    // 내 정보 수정
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyInfo(
            @RequestHeader(name = X_USER_ID) Long userId,
            @RequestBody @Valid MyInfoUpdateRequest request) {

        // 전화번호가 문자수가 넘음
        userManagementService.updateUserName(userId, request.name());
        userManagementService.updatePhoneNumber(userId, request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    // 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @RequestHeader(name = X_USER_ID) Long userId,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        userManagementService.withdraw(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 변경
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(name = X_USER_ID) Long userId,
            @RequestBody @Valid PasswordChangeRequest request) {

        myPageService.updatePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    // 내 권한 목록 조회
    @GetMapping("/me/roles")
    public ResponseEntity<List<MyRoleResponse>> findMyRoles(
            @RequestHeader(name = X_USER_ID) Long userId) {

        List<MyRoleResponse> response = myPageService.findMyRoles(userId);
        return ResponseEntity.ok(response);
    }

    // 연동 소셜 계정 목록
    @GetMapping("/me/oauths")
    public ResponseEntity<List<OauthResponse>> findMyOauths(
            @RequestHeader(name = X_USER_ID) Long userId) {

        List<OauthResponse> response = myPageService.findMyOauths(userId);
        return ResponseEntity.ok(response);
    }

    // 소셜 계정 신규 연동/병합은 브라우저 주도 왕복이 필요해 auth 의 GET /oauth/link/authorize/{provider} 가 담당한다.
    // (provider 동의 화면 → GET /oauth/callback → AuthController 가 상태에 따라 linkOauth 또는 confirmMerge 호출)

    // 소셜 계정 연동 해제
    @DeleteMapping("/me/oauths/{oauthId}")
    public ResponseEntity<Void> unlinkOauth(
            @RequestHeader(name = X_USER_ID) Long userId,
            @PathVariable Long oauthId) {

        User user = userManagementService.findById(userId);
        oauthService.delete(user, oauthId);
        return ResponseEntity.noContent().build();
    }
}
