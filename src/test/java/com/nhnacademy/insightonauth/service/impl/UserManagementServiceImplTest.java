package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.user.*;
import com.nhnacademy.insightonauth.exception.email.*;
import com.nhnacademy.insightonauth.exception.signup.*;
import com.nhnacademy.insightonauth.exception.external.*;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialService userCredentialService;
    @Mock private UserRoleService userRoleService;
    @Mock private OauthService oauthService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private RedisService redisService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private TokenService tokenService;
    @Mock private CoreService coreService;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    // ---------- createUser ----------

    @Test
    @DisplayName("createUser - 이메일 중복이면 예외")
    void createUser_duplicateEmail() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.createUser(
                "test@test.com", "pw", "test", "01012345678", Role.MEMBER, "token"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("createUser - 전화번호 중복이면 예외")
    void createUser_duplicatePhone() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.createUser(
                "test@test.com", "pw", "test", "01012345678", Role.MEMBER, "token"))
                .isInstanceOf(DuplicatePhoneNumberException.class);
    }

    @Test
    @DisplayName("createUser - 성공 시 저장 + 자격증명 + 역할 생성")
    void createUser_success() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(false);

        userManagementService.createUser(
                "test@test.com", "pw", "test", "01012345678", Role.MEMBER, "token");

        verify(emailVerificationService).emailVerifyCheck("test@test.com", "token");
        verify(userRepository).save(any(User.class));
        verify(userCredentialService).create(any(User.class), eq("pw"));
        verify(userRoleService).create(any(User.class), eq(Role.MEMBER));
    }

    // ---------- findById / findReactivatableByEmail ----------

    @Test
    @DisplayName("findById - 없으면 예외")
    void findById_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.findById(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("findReactivatableByEmail - 활성 계정이 있으면 그대로 반환")
    void findReactivatableByEmail_active() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThat(userManagementService.findReactivatableByEmail("test@test.com"))
                .containsSame(user);
        verify(userRepository, never())
                .findByEmailStartingWithAndStatusOrderByWithdrawnAtDesc(anyString(), any());
    }

    @Test
    @DisplayName("findReactivatableByEmail - 활성 없으면 마스킹된 탈퇴 계정을 접두어로 조회")
    void findReactivatableByEmail_withdrawnFallback() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailStartingWithAndStatusOrderByWithdrawnAtDesc(
                "test@test.com;", Status.WITHDRAW)).thenReturn(List.of(user));

        assertThat(userManagementService.findReactivatableByEmail("test@test.com"))
                .containsSame(user);
    }

    @Test
    @DisplayName("findReactivatableByEmail - 둘 다 없으면 빈 값")
    void findReactivatableByEmail_empty() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailStartingWithAndStatusOrderByWithdrawnAtDesc(
                "test@test.com;", Status.WITHDRAW)).thenReturn(List.of());

        assertThat(userManagementService.findReactivatableByEmail("test@test.com")).isEmpty();
    }

    // ---------- reactivate ----------

    @Test
    @DisplayName("reactivate - 탈퇴 복구 기간이 지났으면 예외")
    void reactivate_expired() {
        user.setStatus(Status.WITHDRAW);
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(false);

        assertThatThrownBy(() -> userManagementService.reactivate(user))
                .isInstanceOf(RestorePeriodExpiredException.class);
    }

    @Test
    @DisplayName("reactivate - 원본 이메일이 재사용됐으면 예외")
    void reactivate_emailConflict() {
        user.withdraw();   // email/phone 마스킹, status=WITHDRAW
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(true);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.reactivate(user))
                .isInstanceOf(ReactivationConflictException.class);
    }

    @Test
    @DisplayName("reactivate - 원본 전화번호가 재사용됐으면 예외")
    void reactivate_phoneConflict() {
        user.withdraw();
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(true);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.reactivate(user))
                .isInstanceOf(ReactivationConflictException.class);
    }

    @Test
    @DisplayName("reactivate - 탈퇴 계정 정상 복구 시 oauth 원복 + ACTIVE 전환")
    void reactivate_withdraw_success() {
        user.withdraw();
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(true);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(false);

        userManagementService.reactivate(user);

        verify(oauthService).reactivateByUser(user);
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("reactivate - 휴면 계정은 기간/충돌 검사 없이 ACTIVE 전환")
    void reactivate_sleep_success() {
        user.setStatus(Status.SLEEP);

        userManagementService.reactivate(user);

        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        verify(tokenService, never()).isWithinRestorePeriod(any());
        verify(oauthService, never()).reactivateByUser(any());
    }

    // ---------- withdraw ----------

    @Test
    @DisplayName("withdraw - 이미 탈퇴한 계정이면 예외")
    void withdraw_alreadyWithdrawn() {
        user.setStatus(Status.WITHDRAW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.withdraw(1L, "token"))
                .isInstanceOf(InvalidUserStatusException.class);
    }

    @Test
    @DisplayName("withdraw - Core 호출 실패면 탈퇴 차단")
    void withdraw_coreUnavailable() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(coreService.isGroupManager(1L)).thenThrow(new RuntimeException("core down"));

        assertThatThrownBy(() -> userManagementService.withdraw(1L, "token"))
                .isInstanceOf(CoreServiceUnavailableException.class);
    }

    @Test
    @DisplayName("withdraw - 그룹 관리자면 예외")
    void withdraw_groupManager() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(coreService.isGroupManager(1L)).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.withdraw(1L, "token"))
                .isInstanceOf(ManagerGroupExistsException.class);
    }

    @Test
    @DisplayName("withdraw - 성공 시 마스킹 + 토큰 정리")
    void withdraw_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(coreService.isGroupManager(1L)).thenReturn(false);

        userManagementService.withdraw(1L, "access-token");

        assertThat(user.getStatus()).isEqualTo(Status.WITHDRAW);
        verify(oauthService).maskByUser(user);
        verify(redisService).delete(anyString());
        verify(tokenBlacklistService).blacklistToken("access-token");
    }

    // ---------- 상태 전환 가드 ----------

    @Test
    @DisplayName("sleep - 이미 휴면이면 예외")
    void sleep_alreadySleep() {
        user.setStatus(Status.SLEEP);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.sleep(1L))
                .isInstanceOf(InvalidUserStatusException.class);
    }

    @Test
    @DisplayName("block - 탈퇴 계정은 정지 불가")
    void block_withdrawn() {
        user.setStatus(Status.WITHDRAW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.block(1L))
                .isInstanceOf(InvalidUserStatusException.class);
    }

    @Test
    @DisplayName("block - 성공 시 BLOCK 전환 + access 토큰 무효화 + refresh 삭제")
    void block_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.block(1L);

        assertThat(user.getStatus()).isEqualTo(Status.BLOCK);
        verify(tokenBlacklistService).blacklistByUserId(1L);
        verify(redisService).delete(anyString());
    }

    @Test
    @DisplayName("sleep - 성공 시 SLEEP 전환 + access 토큰 무효화 + refresh 삭제")
    void sleep_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.sleep(1L);

        assertThat(user.getStatus()).isEqualTo(Status.SLEEP);
        verify(tokenBlacklistService).blacklistByUserId(1L);
        verify(redisService).delete(anyString());
    }

    @Test
    @DisplayName("activate - 탈퇴 계정은 활성화 불가")
    void activate_withdrawn() {
        user.setStatus(Status.WITHDRAW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.activate(1L))
                .isInstanceOf(InvalidUserStatusException.class);
    }

    // ---------- deleteUser ----------

    @Test
    @DisplayName("deleteUser - 역할/자격증명/연동/유저 모두 삭제")
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.deleteUser(1L);

        verify(userRoleService).deleteUserRole(user);
        verify(userCredentialService).delete(user);
        verify(oauthService).deleteAllByUser(user);
        verify(userRepository).delete(user);
    }

    // ---------- updatePhoneNumber ----------

    @Test
    @DisplayName("updatePhoneNumber - 다른 번호로 중복이면 예외")
    void updatePhoneNumber_duplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("01099998888")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.updatePhoneNumber(1L, "010-9999-8888"))
                .isInstanceOf(DuplicatePhoneNumberException.class);
    }

    // ---------- findMaskedEmail ----------

    @Test
    @DisplayName("findMaskedEmail - 로컬파트 일부만 노출")
    void findMaskedEmail() {
        when(userRepository.findByUserNameAndPhoneNumber("test", "01012345678"))
                .thenReturn(Optional.of(user));

        String masked = userManagementService.findMaskedEmail("test", "010-1234-5678");

        assertThat(masked).isEqualTo("te**@test.com");
    }

    @Test
    @DisplayName("findMaskedEmail - 유저 없으면 예외")
    void findMaskedEmail_notFound() {
        when(userRepository.findByUserNameAndPhoneNumber("test", "01012345678"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.findMaskedEmail("test", "01012345678"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("findMaskedEmail - 이메일 형식이 올바르지 않으면 예외")
    void findMaskedEmail_invalidFormat() {
        User malformed = new User("not-an-email", "test", "01012345678");
        when(userRepository.findByUserNameAndPhoneNumber("test", "01012345678"))
                .thenReturn(Optional.of(malformed));

        assertThatThrownBy(() -> userManagementService.findMaskedEmail("test", "01012345678"))
                .isInstanceOf(InvalidEmailFormatException.class);
    }
}
