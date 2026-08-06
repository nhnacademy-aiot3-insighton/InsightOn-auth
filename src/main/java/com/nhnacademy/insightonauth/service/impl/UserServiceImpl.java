package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.CoreClient;
import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.core.ManagerGroupExistsResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.email.EmailService;
import com.nhnacademy.insightonauth.entity.*;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.*;
import com.nhnacademy.insightonauth.util.PhoneNumberUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserCredentialService userCredentialService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final EmailService emailService;
    private final OauthClient oauthClient;
    private final OauthService oauthService;
    private final CoreClient coreClient;

    @Override
    public UserSignupResponse createUser(String email, String password, String userName, String phoneNumber, Role role, String verificationToken) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }
        emailService.emailVerifyCheck(email, verificationToken);

        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        if (normalized != null && userRepository.existsByPhoneNumber(normalized)) {
            throw new DuplicatePhoneNumberException("이미 사용 중인 전화번호입니다.");
        }

        User user = new User(email, userName, normalized);
        userRepository.save(user);
        userCredentialService.create(user, password);
        userRoleService.create(user, role);

        return new UserSignupResponse(user.getEmail(), user.getUserName(), user.getPhoneNumber(), user.getCreatedAt());
    }

    @Override
    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    public void emailVerifyRequest(String email) {
        emailService.sendVerificationCode(email);
    }

    @Override
    public String emailVerifyConfirm(String email, String code) {
        return emailService.emailVerify(email, code);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
    }

    @Override
    public UserLoginResponse login(String email, String password) {
        // 로그인 계정 lock 확인
        if (redisService.hasKey(RedisKey.LOGIN_LOCK.getPrefix() + email)) {
            throw new LoginTemporarilyLockedException("5회 연속 로그인 실패로 5분간 잠겼습니다.");
        }

        // 유저 계정 존재 여부 숨기기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("유저를 찾을 수 없습니다."));
        UserCredential credential = userCredentialService.findByUser(user);

        // password 확인
        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            increaseFailCount(email); // 로그인 실패 카운트
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getStatus() == Status.WITHDRAW) {
            if (isWithinRestorePeriod(user)) {
                return handleWithdrawnLogin(user);
            }
            throw new RestorePeriodExpiredException("탈퇴 복구 가능 기간(7일)이 지났습니다.");
        }

        if (!user.getStatus().isLoginable()) {
            throw new InvalidUserException(user.getStatus().getMessage());
        }

        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));

        return issueTokens(user, email);
    }

    //front에서 access, refresh token 제거
    //여기선 refresh만 redis에서 제거
    @Override
    public void logout(Long userId) {
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
    }

    @Override
    public void forceLogout(Long userId) {
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
    }

    @Override
    public void reactivateRequest(String email) {
        emailService.sendVerificationCode(email);
    }

    @Override
    public UserLoginResponse reactivateConfirm(String email, String code) {
        emailService.emailVerify(email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
        user.reactivate();

        return issueTokens(user, email);
    }

    @Override
    public UserLoginResponse reactive(String reactiveToken) {
        String userIdStr = redisService.get(RedisKey.REACTIVE.getPrefix() + reactiveToken);
        if (userIdStr == null) {
            throw new InvalidReactiveTokenException("복구 요청이 유효하지 않거나 만료되었습니다.");
        }

        User user = findById(Long.valueOf(userIdStr));
        user.reactivate();   // 상태를 ACTIVE로, 이메일 원복

        redisService.delete(RedisKey.REACTIVE.getPrefix() + reactiveToken);

        return issueTokens(user, user.getEmail());
    }

    @Override
    public void passwordResetRequest(String email) {
        // 탈퇴 계정은 메일이 나가지 않게 조정, 예외를 던지면 공격자가 계정 존재를 알 수 있음
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() == Status.WITHDRAW) {
                return;
            }
            emailService.sendPasswordResetPath(email);
        });
    }

    @Override
    public void passwordResetConfirm(String token, String newPassword) {
        String email = emailService.emailTokenVerify(token);

        User user = findByEmail(email);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userCredentialService.updatePassword(now, user, newPassword);
        user.setUpdatedAt(now);
    }

    @Override
    public void updateUserName(Long userId, String newUserName) {
        User user = findActiveUser(userId);

        user.setUserName(newUserName);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void updatePhoneNumber(Long userId, String phoneNumber) {
        User user = findActiveUser(userId);
        String normalized = PhoneNumberUtil.normalize(phoneNumber);

        if (normalized != null
                && !normalized.equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(normalized)) {
            throw new DuplicatePhoneNumberException("이미 사용 중인 전화번호입니다.");
        }

        user.setPhoneNumber(normalized);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public String findMaskedEmail(String userName, String phoneNumber) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);

        User user = userRepository.findByUserNameAndPhoneNumber(userName, normalized)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        String email = user.getEmail();

        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        int visibleLength = Math.min(2, local.length() - 1);   // 최소 1글자는 가려지도록
        visibleLength = Math.max(visibleLength, 1);            // 1글자짜리도 앞 1글자는 노출

        // 2글자만 노출 그외 전부 마스킹
        String visible = local.substring(0, visibleLength);
        String masked = "*".repeat(local.length() - visibleLength);

        return visible + masked + domain;
    }

    @Override
    public void updateLastLoginAt(Long userId) {
        User user = findById(userId);
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void activate(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("이미 탈퇴한 계정입니다.");
        }

        user.setStatus(Status.ACTIVE);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void withdraw(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("이미 탈퇴한 계정입니다.");
        }

        ManagerGroupExistsResponse response;
        try {
            response = coreClient.existsManagerGroup(userId);
        } catch (Exception e) {
            log.warn("Core 서비스 호출 실패로 탈퇴를 차단합니다 - userId: {}, 원인: {}", userId, e.getMessage());
            throw new CoreServiceUnavailableException(
                    "일시적으로 그룹 정보를 확인할 수 없어 탈퇴가 제한됩니다. 잠시 후 다시 시도해주세요.");
        }

        if (response.exists()) {
            throw new ManagerGroupExistsException("그룹 관리자 역할이 있어 탈퇴할 수 없습니다.");
        }

        user.withdraw();
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
    }

    @Override
    public void sleep(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.SLEEP) {
            throw new InvalidUserStatusException("이미 휴면 상태 계정입니다.");
        }

        user.setStatus(Status.SLEEP);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void block(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.BLOCK) {
            throw new InvalidUserStatusException("이미 정지된 계정입니다.");
        }


        user.setStatus(Status.BLOCK);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void deleteUser(Long userId) {
        User user = findById(userId);

        // Role 삭제
        userRoleService.deleteUserRole(user);
        // 비밀번호 삭제
        userCredentialService.delete(user);
        // oauth 삭제
        oauthService.deleteAllByUser(user);
        // 유저 삭제
        userRepository.delete(user);
    }

    @Override
    public UserLoginResponse oauthLogin(String provider, String code) {
        OauthUserInfo userInfo = oauthClient.getUserInfo(provider, code);

        Optional<Oauth> existingOauth = oauthService.findByProviderAndProviderUserId(provider, userInfo.providerId());

        if (existingOauth.isPresent()) {
            User user = existingOauth.get().getUser();
            // 탈퇴 상태 체크
            if (user.getStatus() == Status.WITHDRAW) {
                if (isWithinRestorePeriod(user)) {
                    return handleWithdrawnLogin(user);
                }
                throw new RestorePeriodExpiredException("탈퇴 복구 가능 기간(7일)이 지났습니다.");
            }

            if (!user.getStatus().isLoginable()) {
                throw new InvalidUserException(user.getStatus().getMessage());
            }

            return issueTokens(user, user.getEmail());
        }

        User newUser = new User(userInfo.email(), userInfo.name(), null);
        userRepository.save(newUser);
        userRoleService.create(newUser, Role.MEMBER);
        oauthService.create(newUser, provider, userInfo.providerId());

        return issueTokens(newUser, userInfo.email());
    }

    @Override
    public TokenRefreshResponse refresh(Long userId, String refreshToken) {
        try {
            jwtProvider.validateRefreshToken(userId, refreshToken);   // Redis의 jti와 대조 검증
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("유효하지 않은 토큰입니다.");
        }

        User user = findById(userId);
        if (!user.getStatus().isLoginable()) {
            throw new InvalidUserException(user.getStatus().getMessage());
        }

        List<String> roles = userRoleService.findByUser(user).stream()
                .map(userRole -> userRole.getRole().name())
                .toList();

        String accessToken = jwtProvider.createAccessToken(userId, roles);

        return new TokenRefreshResponse(accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findExpiredWithdrawnUsers() {
        return userRepository.findByStatusAndWithdrawnAtBefore(
                Status.WITHDRAW, OffsetDateTime.now(ZoneOffset.UTC).minusDays(90));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findInactiveUsers() {
        return userRepository.findByStatusAndLastLoginAtBefore(
                Status.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC).minusDays(30));
    }

    private User findActiveUser(Long userId) {
        User user = findById(userId);
        if (user.getStatus() != Status.ACTIVE) {
            throw new InvalidUserStatusException("정상 상태의 계정이 아닙니다.");
        }
        return user;
    }

    private void increaseFailCount(String email) {
        String savedFailCount = redisService.get(RedisKey.LOGIN_FAIL.getPrefix() + email);
        int failCount = savedFailCount == null || savedFailCount.isBlank() ? 0 : Integer.parseInt(savedFailCount);
        failCount++;

        if (failCount >= 5) {
            redisService.delete(RedisKey.LOGIN_FAIL.getPrefix() + email);
            redisService.set(RedisKey.LOGIN_LOCK.getPrefix() + email, String.valueOf(failCount), Duration.ofMinutes(5));
            throw new LoginTemporarilyLockedException("5회 연속 로그인 실패로 5분간 잠겼습니다.");
        } else {
            redisService.set(RedisKey.LOGIN_FAIL.getPrefix() + email, String.valueOf(failCount), Duration.ofMinutes(5));
        }
    }

    private UserLoginResponse issueTokens(User user, String email) {
        List<UserRole> userRoleList = userRoleService.findByUser(user);

        String accessToken = jwtProvider.createAccessToken(
                user.getUserId(), userRoleList.stream()
                        .map(userRole -> userRole.getRole().name())
                        .toList());
        String refreshToken = jwtProvider.createRefreshToken(
                user.getUserId(), userRoleList.stream()
                        .map(userRole -> userRole.getRole().name())
                        .toList());

        redisService.delete(RedisKey.LOGIN_LOCK.getPrefix() + email);
        redisService.delete(RedisKey.LOGIN_FAIL.getPrefix() + email);
        return UserLoginResponse.success(accessToken, refreshToken);
    }

    private boolean isWithinRestorePeriod(User user) {
        OffsetDateTime deadline = user.getWithdrawnAt().plusDays(7);
        return OffsetDateTime.now(ZoneOffset.UTC).isBefore(deadline);
    }

    private UserLoginResponse handleWithdrawnLogin(User user) {
        String restoreToken = UUID.randomUUID().toString();
        redisService.set(RedisKey.REACTIVE.getPrefix() + restoreToken,
                user.getUserId().toString(), Duration.ofMinutes(10));

        return UserLoginResponse.pendingRestore(restoreToken);

    }
}
