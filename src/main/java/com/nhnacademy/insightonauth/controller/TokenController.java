package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토큰 상태 조회용 컨트롤러.
 * 사용자 요청이 아니라 게이트웨이가 요청마다 호출하는 service-to-service 엔드포인트로,
 * 액세스 토큰(jti)이 블랙리스트에 올라 있는지 확인한다.
 */
@RestController
@Validated
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenBlacklistService tokenBlacklistService;

    // 주어진 jti(액세스 토큰 ID)가 블랙리스트에 있는지 여부 반환 — 게이트웨이가 인가 전에 호출
    @GetMapping("/tokens/{jti}/blacklisted")
    public ResponseEntity<Boolean> blacklistedCheck(
            @PathVariable("jti") @NotBlank String jti) {
        boolean blacklisted = tokenBlacklistService.isBlacklisted(jti);
        return ResponseEntity.ok(blacklisted);
    }
}
