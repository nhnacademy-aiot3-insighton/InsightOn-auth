package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.dto.OauthUserInfo;
import com.nhnacademy.insightonauth.dto.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.UserSignupResponse;
import com.nhnacademy.insightonauth.email.EmailService;
import com.nhnacademy.insightonauth.entity.*;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nhnacademy.insightonauth.service.UserService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
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
        User user = userRepository.findByUserNameAndPhoneNumber(userName, phoneNumber)
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

        user.withdraw();
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

        // 1. 활성 계정 조회
        Optional<User> activeUser = userRepository.findByEmail(userInfo.email());
        if (activeUser.isPresent()) {
            return issueTokens(activeUser.get(), userInfo.email());
        }

        // 2. 탈퇴 계정 조회 (복구 가능 여부 판단)
        Optional<User> withdrawnUser = userRepository
                .findByEmailStartingWithAndStatus(userInfo.email() + ";", Status.WITHDRAW);

        if (withdrawnUser.isPresent() && isWithinRestorePeriod(withdrawnUser.get())) {
            return handleWithdrawnLogin(withdrawnUser.get());
        }

        // 3. 신규 가입 (탈퇴 이력이 없거나, 있어도 복구 기간이 지난 경우)
        User newUser = new User(userInfo.email(), userInfo.name(), null);
        userRepository.save(newUser);
        userRoleService.create(newUser, Role.MEMBER);
        oauthService.create(newUser, provider, userInfo.providerId());

        return issueTokens(newUser, userInfo.email());
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
        redisService.set(RedisKey.RESTORE.getPrefix() + restoreToken,
                user.getUserId().toString(), Duration.ofMinutes(10));

        return UserLoginResponse.pendingRestore(restoreToken);

    }
}
