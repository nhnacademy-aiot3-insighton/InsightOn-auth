package com.nhnacademy.insightonauth.provider;

import com.nhnacademy.insightonauth.exception.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.RefreshTokenNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void setUp() throws Exception {
        redisService = mock(RedisService.class);
        jwtProvider = new JwtProvider(privateKeyB64, publicKeyB64, "test-key-v1",
                Duration.ofMinutes(15), Duration.ofDays(15), null, redisService);
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
    @DisplayName("createRefreshToken — 토큰 발급 + Redis에 jti 저장")
    void createRefreshToken() {
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));

        Claims c = jwtProvider.parse(token);
        assertThat(c.getSubject()).isEqualTo("1");
        verify(redisService).set(eq("refresh:1"), eq(c.getId()), eq(Duration.ofDays(15)));
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
    void parse_expired() throws Exception {
        JwtProvider shortLived = new JwtProvider(privateKeyB64, publicKeyB64, "test-key-v1",
                Duration.ofSeconds(-1), Duration.ofDays(1), null, redisService);
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
                Duration.ofMinutes(15), Duration.ofDays(15), null, redisService);
        String token = foreign.createAccessToken(1L, List.of("MEMBER"), "n");

        assertThatThrownBy(() -> jwtProvider.parse(token)).isInstanceOf(JwtException.class);
    }

    // ---------- validateRefreshToken ----------

    @Test
    @DisplayName("validateRefreshToken — 정상이면 예외 없음")
    void validateRefreshToken_ok() {
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));
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
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(2L, token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("validateRefreshToken — Redis에 jti 없으면 RefreshTokenNotFoundException")
    void validateRefreshToken_noRedisEntry() {
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));
        when(redisService.get("refresh:1")).thenReturn(null);

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(1L, token))
                .isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    @DisplayName("validateRefreshToken — Redis jti가 토큰 jti와 다르면 InvalidRefreshTokenException")
    void validateRefreshToken_jtiRotated() {
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));
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
        String token = jwtProvider.createRefreshToken(1L, List.of("MEMBER"));
        assertThat(jwtProvider.extractRoles(token)).isEmpty();
    }

    @Test
    @DisplayName("hasAdminRole — null 토큰이면 현재 IllegalArgumentException (로그인 500 버그의 원인)")
    void hasAdminRole_null_currentBehavior() {
        // extractRoles가 parse(null)의 IllegalArgumentException을 잡지 않음.
        // 복구기간 내 탈퇴계정이 비밀번호 로그인하면 accessToken=null로 여기 도달 → 500.
        // 버그 픽스 시 이 테스트를 "false 반환" 기대로 바꿀 것.
        assertThatThrownBy(() -> jwtProvider.hasAdminRole(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
