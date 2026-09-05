package com.nhnacademy.insightonauth.provider;

import com.nhnacademy.insightonauth.exception.auth.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.auth.RefreshTokenNotFoundException;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class JwtProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;
    @Getter
    private final Duration accessValidity;
    private final Duration refreshValidity;
    private final RedisService redisService;

    public JwtProvider(
            @Value("${jwt.private-key}") String privateKeyBase64,
            @Value("${jwt.public-key}") String publicKeyBase64,
            @Value("${jwt.key-id}") String keyId,
            @Value("${jwt.access-token-validity}") Duration accessValidity,
            @Value("${jwt.refresh-token-validity}") Duration refreshValidity,
            RedisService redisService) throws Exception {
        this.privateKey = loadPrivateKey(privateKeyBase64);
        this.publicKey = loadPublicKey(publicKeyBase64);
        this.keyId = keyId;
        this.accessValidity = accessValidity;
        this.refreshValidity = refreshValidity;
        this.redisService = redisService;
    }

    public String createAccessToken(Long userId, List<String> roles, String userName) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject(userId.toString())
                .id(jti) //jti
                .claim("roles", roles)
                .claim("name", userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessValidity)))
                .signWith(privateKey)
                .compact();

        // userId → 현재 access jti. 강제 로그아웃/정지 시 이 값으로 블랙리스트에 올린다.
        // 새 jti 를 저장하면서 직전 jti 를 원자적으로 받아 즉시 블랙리스트에 등록한다.
        // → 로그인·refresh·동시 로그인(이중화 포함) 어느 경로로 재발급되든
        //   "유저별 살아있는 access jti 는 항상 1개" 가 유지된다 (last-wins).
        String previousJti = redisService.setAndGetPrevious(
                RedisKey.ACCESS_JTI.getPrefix() + userId, jti, accessValidity);
        if (previousJti != null && !previousJti.equals(jti)) {
            redisService.set(RedisKey.BLACKLIST.getPrefix() + previousJti, "1", accessValidity);
        }
        return token;
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject(userId.toString())
                .id(jti) //jti
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshValidity)))
                .signWith(privateKey)
                .compact();

        redisService.set(RedisKey.REFRESH.getPrefix() + userId, jti, refreshValidity);
        return token;
    }

    public void validateRefreshToken(Long userId, String token) {
        //토큰 검증
        Claims claims;
        try {
            claims = parse(token);
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("유효하지 않은 토큰 입니다.");
        }
        // 토큰 userId 검증
        if (!claims.getSubject().equals(userId.toString())) {
            throw new InvalidRefreshTokenException("토큰 소유자가 일치하지 않습니다.");
        }

        //redis 존재 검사
        String jti = redisService.get(RedisKey.REFRESH.getPrefix() + userId);
        if(jti == null) {
            throw new RefreshTokenNotFoundException("토큰을 찾을 수 없습니다.");
        }
        if (!jti.equals(claims.getId())) {
            throw new InvalidRefreshTokenException("유효하지 않은 토큰 입니다.");
        }
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)  // public key로 검증 설정
                .build()                // parser 완성
                .parseSignedClaims(token)   // 토큰 검증
                .getPayload();
    }

    private PrivateKey loadPrivateKey(String base64Key) throws Exception {
        String pemText = new String(Base64.getDecoder().decode(base64Key));

        String content = pemText
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(String base64Key) throws Exception {
        String pemText = new String(Base64.getDecoder().decode(base64Key));

        String content = pemText
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }

    /**
     * accessToken의 roles 클레임에 ADMIN이 포함되어 있는지 타입 안전하게 검사한다.
     * 토큰이 null/빈 문자열이거나(예: 복구 대기 응답), roles가 없거나 형식이 예상과 다르면 false를 반환한다.
     */
    public boolean hasAdminRole(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            // 정상 경로: PENDING_RESTORE 응답은 컨트롤러에서 이미 걸러지므로 여기 도달하지 않는다.
            // 여기 도달했다면 다른 곳에서 토큰 없이 admin 검사를 호출한 것 → 관리자 아님으로 처리하되 흔적을 남긴다.
            log.warn("hasAdminRole 에 null/빈 accessToken 전달됨 — 비관리자로 처리");
            return false;
        }
        return extractRoles(accessToken).contains("ADMIN");
    }

    /**
     * accessToken에서 roles 클레임을 문자열 리스트로 안전하게 추출한다.
     * 클레임이 없거나 List 타입이 아니면 빈 리스트를 반환한다.
     */
    public List<String> extractRoles(String accessToken) {
        Object claim = parse(accessToken).get("roles");
        if (!(claim instanceof List<?> rawList)) {
            return List.of();   // roles 없음 / 타입 불일치 → 빈 리스트 (ClassCastException 방지)
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)   // enum/String 무엇이든 문자열로 통일
                .toList();
    }

}
