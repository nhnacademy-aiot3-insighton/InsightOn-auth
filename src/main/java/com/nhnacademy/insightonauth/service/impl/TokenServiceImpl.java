package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.TokenService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserRoleService userRoleService;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Override
    public UserLoginResponse issueTokens(User user, String email) {
        List<UserRole> userRoleList = userRoleService.findByUser(user);
        List<String> roles = userRoleList.stream()
                .map(userRole -> userRole.getRole().name())
                .toList();

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), roles, user.getUserName());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), roles);

        redisService.delete(RedisKey.LOGIN_LOCK.getPrefix() + email);
        redisService.delete(RedisKey.LOGIN_FAIL.getPrefix() + email);
        return UserLoginResponse.success(accessToken, refreshToken);
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

    @Override
    public UserLoginResponse handleWithdrawnLogin(User user) {
        String restoreToken = UUID.randomUUID().toString();
        redisService.set(RedisKey.REACTIVE.getPrefix() + restoreToken,
                user.getUserId().toString(), Duration.ofMinutes(10));

        return UserLoginResponse.pendingRestore(restoreToken);
    }
}
