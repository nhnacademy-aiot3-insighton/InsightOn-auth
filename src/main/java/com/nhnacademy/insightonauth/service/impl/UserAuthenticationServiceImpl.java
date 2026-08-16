package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.client.OauthClientResolver;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.entity.*;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.redis.ResendCounter;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.*;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

    // Transactional 전파 필요없으면 빼기 어노테이션 붙이기
    // private 메소드의 Transactional의 붙는 경우 proxy가 적용안되나 현재 private은 사용이 불필요
    private final UserRepository userRepository;
    private final UserCredentialService userCredentialService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final OauthClientResolver oauthClientResolver;
    private final OauthService oauthService;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenService tokenService;
    private final UserManagementService userManagementService;
    private final ResendCounter resendCounter;

    @Override
    public UserLoginResponse login(String email, String password) {
        // 로그인 계정 lock 확인
        if (redisService.hasKey(RedisKey.LOGIN_LOCK.getPrefix() + email)) {
            throw new LoginTemporarilyLockedException("5회 연속 로그인 실패로 5분간 잠겼습니다.");
        }

        // 유저 계정 존재 여부 숨기기
        // 기존대로 UserNotFound로 하는건 어떤가
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("유저를 찾을 수 없습니다."));
        UserCredential credential = userCredentialService.findByUser(user);

        // password 확인
        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            increaseFailCount(email); // 로그인 실패 카운트
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getStatus() == Status.WITHDRAW) {
            if (tokenService.isWithinRestorePeriod(user)) {
                return tokenService.handleWithdrawnLogin(user);
            }
            throw new RestorePeriodExpiredException("탈퇴 복구 가능 기간(7일)이 지났습니다.");
        }

        if (!user.getStatus().isLoginable()) {
            throw new InvalidUserException(user.getStatus().getMessage());
        }

        user.updateLastLoginAt();

        return tokenService.issueTokens(user, email);
    }

    //front에서 access, refresh token 제거
    //여기선 refresh만 redis에서 제거
    @Override
    public void logout(Long userId, String accessToken) {
        // 리프레시 삭제
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);

        // 블랙리스트 등록
        tokenBlacklistService.blacklistToken(accessToken);
    }

    @Override
    public void forceLogout(Long userId) {
        // 강제 로그아웃의 경우 access를 짧게 주었기 때문에 리프레시 삭제만으로 일단 결정
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
    }

    @Override
    public UserLoginResponse oauthLogin(String provider, String code) {
        OauthClient oauthClient = oauthClientResolver.resolve(provider);
        OauthUserInfo userInfo = oauthClient.getUserInfo(code);

        Optional<Oauth> existingOauth = oauthService.findByProviderAndProviderUserId(provider, userInfo.providerId());

        if (existingOauth.isPresent()) {
            User user = existingOauth.get().getUser();
            // 탈퇴 상태 체크
            if (user.getStatus() == Status.WITHDRAW) {
                if (tokenService.isWithinRestorePeriod(user)) {
                    return tokenService.handleWithdrawnLogin(user);
                }
                throw new RestorePeriodExpiredException("탈퇴 복구 가능 기간(7일)이 지났습니다.");
            }

            if (!user.getStatus().isLoginable()) {
                throw new InvalidUserException(user.getStatus().getMessage());
            }

            user.updateLastLoginAt();
            return tokenService.issueTokens(user, user.getEmail());
        }

        // 새 User 만들기 전에, 이 이메일이 이미 가입돼 있는지 확인
        if (userRepository.existsByEmail(userInfo.email())) {
            // 이미 이 이메일로 가입된 계정이 있음 → 자동 생성/연결하지 않고 차단
            throw new EmailAlreadyRegisteredException(
                    "이미 가입된 이메일입니다. 로그인 후 마이페이지에서 소셜 계정을 연동해 주세요.");
        }

        User newUser = new User(userInfo.email(), userInfo.name(), null);
        userRepository.save(newUser);
        userRoleService.create(newUser, Role.MEMBER);
        oauthService.create(newUser, provider, userInfo.providerId());
        newUser.updateLastLoginAt();

        return tokenService.issueTokens(newUser, userInfo.email());
    }

    @Override
    public TokenRefreshResponse refresh(Long userId, String refreshToken) {
        try {
            jwtProvider.validateRefreshToken(userId, refreshToken);   // Redis의 jti와 대조 검증
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("유효하지 않은 토큰입니다.");
        }

        User user = userManagementService.findById(userId);
        if (!user.getStatus().isLoginable()) {
            throw new InvalidUserException(user.getStatus().getMessage());
        }

        List<String> roles = userRoleService.findByUser(user).stream()
                .map(userRole -> userRole.getRole().name())
                .toList();

        String accessToken = jwtProvider.createAccessToken(userId, roles);

        return new TokenRefreshResponse(accessToken);
    }

    private void increaseFailCount(String email) {
        boolean lockedNow = resendCounter.increase(
                RedisKey.LOGIN_FAIL.getPrefix() + email,
                RedisKey.LOGIN_LOCK.getPrefix() + email,
                5,
                Duration.ofMinutes(5)
        );

        if (lockedNow) {
            throw new LoginTemporarilyLockedException("5회 연속 로그인 실패로 5분간 잠겼습니다.");
        }
    }
}
