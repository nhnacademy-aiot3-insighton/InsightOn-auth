package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    @Test
    @DisplayName("블랙리스트에 있으면 true 반환")
    void isBlacklisted_true() {
        when(redisService.hasKey(RedisKey.BLACKLIST.getPrefix() + "jti")).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted("jti");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("블랙리스트에 없으면 false 반환")
    void isBlacklisted_false() {
        when(redisService.hasKey(RedisKey.BLACKLIST.getPrefix() + "jti")).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted("jti");

        assertThat(result).isFalse();

    }

    @Test
    @DisplayName("정상 토큰이면 블랙리스트에 등록됨")
    void blacklistToken_valid() {
        String token = "accessToken";
        Claims claims = mock(Claims.class);
        when(jwtProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn("jti");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + Duration.ofMinutes(5).toMillis()));

        tokenBlacklistService.blacklistToken(token);

        verify(redisService).set(eq(RedisKey.BLACKLIST.getPrefix() + "jti"), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("null 토큰이면 등록 없이 스킵됨")
    void blacklistToken_nullToken() {
        String token = null;

        tokenBlacklistService.blacklistToken(token);

        verify(redisService, never()).set(any(), anyString(), any());
    }

    @Test
    @DisplayName("빈 토큰이면 등록 없이 스킵됨")
    void blacklistToken_blankToken() {
        String token = "  ";

        tokenBlacklistService.blacklistToken(token);

        verify(redisService, never()).set(any(), anyString(), any());
    }

    @Test
    @DisplayName("만료된 토큰이면 등록 없이 스킵됨")
    void blacklistToken_expiredToken() {
        String token = "expired-token";
        when(jwtProvider.parse(token)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        tokenBlacklistService.blacklistToken(token);

        verify(redisService, never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 등록 없이 스킵됨")
    void blacklistToken_invalidToken() {
        String token = "invalid-token";
        when(jwtProvider.parse(token)).thenThrow(new MalformedJwtException("invalid"));

        tokenBlacklistService.blacklistToken(token);

        verify(redisService, never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 등록 없이 스킵됨")
    void blacklistToken_illegalArgumentToken() {
        String token = "malformed-token";
        when(jwtProvider.parse(token)).thenThrow(new IllegalArgumentException("잘못된 토큰 형식"));

        tokenBlacklistService.blacklistToken(token);

        verify(redisService, never()).set(anyString(), anyString(), any());
    }
}
