package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Override
    public boolean isBlacklisted(String jti) {
        return redisService.hasKey(RedisKey.BLACKLIST.getPrefix() + jti);
    }

    @Override
    public void blacklistToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.debug("빈 토큰이라 블랙리스트 등록을 건너뜁니다.");
            return;
        }

        try {
            Claims claims = jwtProvider.parse(accessToken);
            String jti = claims.getId();
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                redisService.set(RedisKey.BLACKLIST.getPrefix() + jti, "1", Duration.ofMillis(remainingMs));
            }
        } catch (ExpiredJwtException e) {
            // 이미 만료된 토큰 — 블랙리스트 등록 불필요, 조용히 넘어감
            log.debug("이미 만료된 토큰이라 블랙리스트 등록을 건너뜁니다.");
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 오류 등 기타 유효하지 않은 토큰 — 마찬가지로 무시 가능
            log.warn("유효하지 않은 토큰으로 블랙리스트 등록 시도: {}", e.getMessage());
        }
    }
}
