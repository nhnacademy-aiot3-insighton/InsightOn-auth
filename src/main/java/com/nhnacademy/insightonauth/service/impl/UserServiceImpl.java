package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.*;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nhnacademy.insightonauth.service.UserService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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

    @Override
    public UserSignupResponse createUser(String email, String password, String userName, String phoneNumber, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

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
    public boolean emailVerify(String email) {


        return false;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
        UserCredential credential = userCredentialService.findByUser(user);

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.getStatus().isLoginable()) {
            throw new InvalidUserException(user.getStatus().getMessage());
        }

        List<UserRole> userRoleList = userRoleService.findByUser(user);

        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));

        String accessToken = jwtProvider.createAccessToken(
                user.getUserId(), userRoleList.stream()
                        .map(userRole -> userRole.getRole().name())
                        .toList());
        String refreshToken = jwtProvider.createRefreshToken(
                user.getUserId(), userRoleList.stream()
                .map(userRole -> userRole.getRole().name())
                .toList());

        return new UserLoginResponse(accessToken, refreshToken);
    }

    //front에서 access, refresh token 제거
    //여기선 refresh만 redis에서 제거
    @Override
    public void logout(Long userId) {
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
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
    public void updateLastLoginAt(Long userId) {
        User user = findById(userId);
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void activate(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new IllegalStateException("이미 탈퇴한 계정입니다.");
        }

        user.setStatus(Status.ACTIVE);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void withdraw(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new IllegalStateException("이미 탈퇴한 계정입니다.");
        }

        user.withdraw();
    }

    @Override
    public void sleep(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.SLEEP) {
            throw new IllegalStateException("이미 휴면 상태 계정입니다.");
        }

        user.setStatus(Status.SLEEP);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void block(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.BLOCK) {
            throw new IllegalStateException("이미 정지된 계정입니다.");
        }


        user.setStatus(Status.BLOCK);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void deleteUser(Long userId) {
        User user = findById(userId);

        userRepository.delete(user);
    }

    private User findActiveUser(Long userId) {
        User user = findById(userId);
        if (user.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("정상 상태의 계정이 아닙니다.");
        }
        return user;
    }
}
