package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.CoreClient;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.core.ManagerGroupResponse;
import com.nhnacademy.insightonauth.email.EmailService;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.*;
import com.nhnacademy.insightonauth.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserCredentialService userCredentialService;
    private final UserRoleService userRoleService;
    private final OauthService oauthService;
    private final EmailService emailService;
    private final RedisService redisService;
    private final TokenBlacklistService tokenBlacklistService;
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
    public void activate(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("이미 탈퇴한 계정입니다.");
        }

        user.setStatus(Status.ACTIVE);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    //탈톼시 비밀번호 확인
    @Override
    public void withdraw(Long userId, String accessToken) {
        User user = findById(userId);

        if (user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("이미 탈퇴한 계정입니다.");
        }

        ManagerGroupResponse response;
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
        tokenBlacklistService.blacklistToken(accessToken);
    }

    // block 계정도 sleep으로 해도되나
    @Override
    public void sleep(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.SLEEP) {
            throw new InvalidUserStatusException("이미 휴면 상태 계정입니다.");
        }
        if (user.getStatus() == Status.BLOCK || user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("차단되었거나 탈퇴한 계정은 휴면 전환할 수 없습니다.");
        }

        user.setStatus(Status.SLEEP);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // 차단 시 리프레시 삭제 — 재발급 차단
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
    }

    @Override
    public void block(Long userId) {
        User user = findById(userId);

        if (user.getStatus() == Status.BLOCK) {
            throw new InvalidUserStatusException("이미 정지된 계정입니다.");
        }

        if (user.getStatus() == Status.WITHDRAW) {
            throw new InvalidUserStatusException("탈퇴한 계정은 정지할 수 없습니다.");
        }

        user.setStatus(Status.BLOCK);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // 차단 시 리프레시 삭제 — 재발급 차단
        redisService.delete(RedisKey.REFRESH.getPrefix() + userId);
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

    // 전화번호 찾기시 이름, 전화번호가 충분한 인증인가 생각해보기
    @Override
    public String findMaskedEmail(String userName, String phoneNumber) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);

        User user = userRepository.findByUserNameAndPhoneNumber(userName, normalized)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        String email = user.getEmail();
        if (!email.contains("@")) {
            throw new InvalidEmailFormatException("올바르지 않은 이메일 형식입니다.");
        }

        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        // 이메일이 1글자면 전체 마스킹
        int visibleLength = local.length() == 1 ? 0 : Math.min(2, local.length() - 1);

        // 2글자만 노출 그외 전부 마스킹
        String visible = local.substring(0, visibleLength);
        String masked = "*".repeat(local.length() - visibleLength);

        return visible + masked + domain;
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
}
