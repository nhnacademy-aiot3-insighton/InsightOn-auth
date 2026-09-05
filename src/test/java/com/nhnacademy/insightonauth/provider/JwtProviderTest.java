package com.nhnacademy.insightonauth.provider;

import com.nhnacademy.insightonauth.exception.auth.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.auth.RefreshTokenNotFoundException;
import com.nhnacademy.insightonauth.redis.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtProvider 단위 테스트 — 토큰 생성/파싱/검증, refresh 토큰 Redis 연동, 역할 추출.
 * 테스트 전용 RSA 키쌍을 즉석에서 생성해 사용한다.
 */
class JwtProviderTest {

    private static String privateKeyB64;
    private static String publicKeyB64;

    private RedisService redisService;
    private JwtProvider jwtProvider;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String pubPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        privateKeyB64 = Base64.getEncoder().encodeToString(privPem.getBytes());
        publicKeyB64 = Base64.getEncoder().encodeToString(pubPem.getBytes());
    }

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        jwtProvider = new JwtProvider(privateKeyB64, publicKeyB64, "test-key-v1",
                Duration.ofMinutes(15), Duration.ofDays(15), redisService);
    }

    // ---------- 생성 / 파싱 ----------

    @Test
    @DisplayName("createAccessToken — subject/roles/name/jti/만료 클레임 포함")
    void createAccessToken() {
        String token = jwtProvider.createAccessToken(1L, List.of("MEMBER", "ADMIN"), "홍길동");

        Claims c = jwtProvider.parse(token);
        assertThat(c.getSubject()).isEqualTo("1");
        assertThat(jwtProvider.extractRoles(token)).containsExactly("MEMBER", "ADMIN");
        assertThat(c.get("name", String.class)).isEqualTo("홍길동");
        assertThat(c.getId()).isNotBlank();
        assertThat(c.getExpiration()).isAfter(c.getIssuedAt());
    }

    @Test
    @DisplayName("createAccessToken — 새 jti 를 access-jti:{userId} 에 저장 (직전 값 원자적 조회)")
    void createAccessToken_storesAccessJti() {
        String token = jwtProvider.createAccessToken(1L, List.of("MEMBER"), "n");

        String jti = jwtProvider.parse(token).getId();
        verify(redisService).setAndGetPrevious("access-jti:1", jti, Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("createAccessToken — 직전 access jti 를 즉시 블랙리스트에 등록 (last-wins)")
    void createAccessToken_blacklistsPreviousJti() {
        when(redisService.setAndGetPrevious(eq("access-jti:1"), anyString(), any()))
                .thenReturn("previous-jti");

        jwtProvider.createAccessToken(1L, List.of("MEMBER"), "n");

        verify(redisService).set("blacklist:previous-jti", "1", Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("createAccessToken — 직전 jti 가 없으면 블랙리스트 등록 안 함")
    void createAccessToken_noPreviousJti() {
        when(redisService.setAndGetPrevious(eq("access-jti:1"), anyString(), any())).thenReturn(null);

        jwtProvider.createAccessToken(1L, List.of("MEMBER"), "n");

        verify(redisService, never()).set(startsWith("blacklist:"), anyString(), any());
    }

    @Test
    @DisplayName("createRefreshToken — 토큰 발급 + Redis에 jti 저장")
    void createRefreshToken() {
        String token = jwtProvider.createRefreshToken(1L);

        Claims c = jwtProvider.parse(token);
        assertThat(c.getSubject()).isEqualTo("1");
        verify(redisService).set("refresh:1", c.getId(), Duration.ofDays(15));
    }

    @Test
    @DisplayName("parse — 위조된 토큰이면 JwtException")
    void parse_tampered() {
        String token = jwtProvider.createAccessToken(1L, List.of("MEMBER"), "n");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtProvider.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse — 만료된 토큰이면 JwtException")
    void parse_expired() {
        JwtProvider shortLived = new JwtProvider(privateKeyB64, publicKeyB64, "test-key-v1",
                Duration.ofSeconds(-1), Duration.ofDays(1), redisService);
        String token = shortLived.createAccessToken(1L, List.of("MEMBER"), "n");

        assertThatThrownBy(() -> jwtProvider.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse — 다른 키로 서명된 토큰이면 JwtException")
    void parse_wrongKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair other = kpg.generateKeyPair();
        String otherPrivB64 = Base64.getEncoder().encodeToString(
                ("-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getMimeEncoder().encodeToString(other.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----\n").getBytes());
        JwtProvider foreign = new JwtProvider(otherPrivB64, publicKeyB64, "test-key-v1",
                Duration.ofMinutes(15), Duration.ofDays(15), redisService);
        String token = foreign.createAccessToken(1L, List.of("MEMBER"), "n");

        assertThatThrownBy(() -> jwtProvider.parse(token)).isInstanceOf(JwtException.class);
    }

    // ---------- validateRefreshToken ----------

    @Test
    @DisplayName("validateRefreshToken — 정상이면 예외 없음")
    void validateRefreshToken_ok() {
        String token = jwtProvider.createRefreshToken(1L);
        String jti = jwtProvider.parse(token).getId();
        when(redisService.get("refresh:1")).thenReturn(jti);

        assertThatCode(() -> jwtProvider.validateRefreshToken(1L, token)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRefreshToken — 파싱 실패 시 InvalidRefreshTokenException")
    void validateRefreshToken_parseFail() {
        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(1L, "not-a-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("validateRefreshToken — 다른 userId의 토큰이면 InvalidRefreshTokenException")
    void validateRefreshToken_subjectMismatch() {
        String token = jwtProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(2L, token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("validateRefreshToken — Redis에 jti 없으면 RefreshTokenNotFoundException")
    void validateRefreshToken_noRedisEntry() {
        String token = jwtProvider.createRefreshToken(1L);
        when(redisService.get("refresh:1")).thenReturn(null);

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(1L, token))
                .isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    @DisplayName("validateRefreshToken — Redis jti가 토큰 jti와 다르면 InvalidRefreshTokenException")
    void validateRefreshToken_jtiRotated() {
        String token = jwtProvider.createRefreshToken(1L);
        when(redisService.get("refresh:1")).thenReturn("rotated-jti");

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(1L, token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ---------- hasAdminRole / extractRoles ----------

    @Test
    @DisplayName("hasAdminRole — roles에 ADMIN 있으면 true")
    void hasAdminRole_true() {
        String token = jwtProvider.createAccessToken(1L, List.of("MEMBER", "ADMIN"), "n");
        assertThat(jwtProvider.hasAdminRole(token)).isTrue();
    }

    @Test
    @DisplayName("hasAdminRole — roles에 ADMIN 없으면 false")
    void hasAdminRole_false() {
        String token = jwtProvider.createAccessToken(1L, List.of("MEMBER"), "n");
        assertThat(jwtProvider.hasAdminRole(token)).isFalse();
    }

    @Test
    @DisplayName("extractRoles — roles 클레임이 없으면 빈 리스트 (refresh 토큰)")
    void extractRoles_absent() {
        String token = jwtProvider.createRefreshToken(1L);
        assertThat(jwtProvider.extractRoles(token)).isEmpty();
    }

    @Test
    @DisplayName("hasAdminRole — null/빈 토큰이면 false (복구 대기 응답 등)")
    void hasAdminRole_nullOrBlank() {
        // 복구기간 내 탈퇴계정이 비밀번호 로그인하면 accessToken=null 로 여기 도달할 수 있다.
        // 예외 대신 false 를 반환해 컨트롤러의 500 을 막는다.
        assertThat(jwtProvider.hasAdminRole(null)).isFalse();
        assertThat(jwtProvider.hasAdminRole("")).isFalse();
        assertThat(jwtProvider.hasAdminRole("   ")).isFalse();
    }
}
