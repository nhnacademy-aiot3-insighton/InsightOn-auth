package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private TokenServiceImpl tokenService;

    User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Test
    @DisplayName("역할이 하나면 토큰 발급됨")
    void issueTokens_singleRole() {
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.MEMBER)));
        when(jwtProvider.createAccessToken(1L, List.of("MEMBER"), "test")).thenReturn("accessToken");
        when(jwtProvider.createRefreshToken(1L, List.of("MEMBER"))).thenReturn("refreshToken");

        UserLoginResult userLoginResult = tokenService.issueTokens(user, user.getEmail());

        assertThat(userLoginResult.accessToken()).isEqualTo("accessToken");
        assertThat(userLoginResult.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("역할이 여러 개면 모두 포함해 토큰 발급됨")
    void issueTokens_multiRole() {
        when(userRoleService.findByUser(user))
                .thenReturn(List.of(new UserRole(user, Role.ADMIN), new UserRole(user, Role.MEMBER)));
        when(jwtProvider.createAccessToken(1L, List.of("ADMIN", "MEMBER"), "test")).thenReturn("accessToken");
        when(jwtProvider.createRefreshToken(1L, List.of("ADMIN", "MEMBER"))).thenReturn("refreshToken");

        UserLoginResult userLoginResult = tokenService.issueTokens(user, user.getEmail());

        assertThat(userLoginResult.accessToken()).isEqualTo("accessToken");
        assertThat(userLoginResult.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("7일 이내 탈퇴면 복구 가능함")
    void isWithinRestorePeriod_true() {
        ReflectionTestUtils.setField(user, "withdrawnAt", OffsetDateTime.now(ZoneOffset.UTC).minusDays(3));

        assertThat(tokenService.isWithinRestorePeriod(user)).isTrue();
    }

    @Test
    @DisplayName("7일 지난 탈퇴면 복구 불가능함")
    void isWithinRestorePeriod_false() {
        ReflectionTestUtils.setField(user, "withdrawnAt", OffsetDateTime.now(ZoneOffset.UTC).minusDays(8));

        assertThat(tokenService.isWithinRestorePeriod(user)).isFalse();
    }

    @Test
    @DisplayName("탈퇴 시각이 없으면 복구 불가능함")
    void isWithinRestorePeriod_null() {
        ReflectionTestUtils.setField(user, "withdrawnAt", null);

        assertThat(tokenService.isWithinRestorePeriod(user)).isFalse();
    }

    @Test
    @DisplayName("복구 토큰 발급 및 Redis 저장됨")
    void handleWithdrawnLogin() {
        UserLoginResult response = tokenService.handleWithdrawnLogin(user);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).set(keyCaptor.capture(), eq("1"), eq(Duration.ofMinutes(10)));

        assertThat(keyCaptor.getValue())
                .isEqualTo(RedisKey.REACTIVE.getPrefix() + response.restoreToken());
    }
}
