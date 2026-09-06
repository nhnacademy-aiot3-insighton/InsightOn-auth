package com.nhnacademy.insightonauth.controller.api;

import com.nhnacademy.insightonauth.controller.swagger.TokenApi;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
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
public class TokenController implements TokenApi {

    private final TokenBlacklistService tokenBlacklistService;

    // 주어진 jti(액세스 토큰 ID)가 블랙리스트에 있는지 여부 반환 — 게이트웨이가 인가 전에 호출
    @Override
    @GetMapping("/tokens/{jti}/blacklisted")
    public ResponseEntity<Boolean> blacklistedCheck(
            @PathVariable("jti") String jti) {
        boolean blacklisted = tokenBlacklistService.isBlacklisted(jti);
        return ResponseEntity.ok(blacklisted);
    }
}
