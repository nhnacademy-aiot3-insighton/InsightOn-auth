package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.TokenService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserRoleService userRoleService;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Override
    public UserLoginResult issueTokens(User user, String email) {
        List<UserRole> userRoleList = userRoleService.findByUser(user);
        List<String> roles = userRoleList.stream()
                .map(userRole -> userRole.getRole().name())
                .toList();

        // 동시 로그인 차단(last-wins)은 createAccessToken 이 처리한다 —
        // 직전 access jti 를 원자적으로 블랙리스트에 올리고, createRefreshToken 이
        // refresh:{userId} 를 덮어쓴다. 밀려난 세션은 다음 요청에서 401 → refresh 실패로 로그아웃.
        String accessToken = jwtProvider.createAccessToken(user.getUserId(), roles, user.getUserName());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        redisService.delete(RedisKey.LOGIN_LOCK.getPrefix() + email);
        redisService.delete(RedisKey.LOGIN_FAIL.getPrefix() + email);
        return UserLoginResult.success(accessToken, refreshToken);
    }

    @Override
    public boolean isWithinRestorePeriod(User user) {
        OffsetDateTime withdrawnAt = user.getWithdrawnAt();
        if (withdrawnAt == null) {
            return false;   // 탈퇴 시각이 없으면 복구 불가로 처리 (안전한 기본값)
        }
        OffsetDateTime deadline = withdrawnAt.plusDays(7);
        return OffsetDateTime.now(ZoneOffset.UTC).isBefore(deadline);
    }
}
