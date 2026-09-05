package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.exception.user.InvalidUserRoleException;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuthenticationService userAuthenticationService;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Test
    @DisplayName("status가 있으면 status 조건 조회 사용")
    void findUsers_withStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailContainingAndUserNameContainingAndStatus(
                "test", "test", Status.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminFindUsersResponse> result =
                adminUserService.findUsers("test", "test", Status.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("status가 null이면 status 없는 조회 사용, email/userName null은 빈 문자열로")
    void findUsers_withoutStatus_nullConditions() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailContainingAndUserNameContaining("", "", pageable))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminFindUsersResponse> result =
                adminUserService.findUsers(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByEmailContainingAndUserNameContaining("", "", pageable);
    }

    @Test
    @DisplayName("유저 상세 조회 시 역할 목록 포함")
    void findUserDetail() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user))
                .thenReturn(List.of(new UserRole(user, Role.MEMBER), new UserRole(user, Role.ADMIN)));

        AdminUserDetailResponse result = adminUserService.findUserDetail(1L);

        assertThat(result.email()).isEqualTo("test@test.com");
        assertThat(result.roles()).containsExactlyInAnyOrder(Role.MEMBER, Role.ADMIN);
    }

    @Test
    @DisplayName("block/sleep/activate는 UserManagementService에 위임")
    void statusChanges_delegate() {
        adminUserService.block(1L);
        adminUserService.sleep(1L);
        adminUserService.activate(1L);

        verify(userManagementService).block(1L);
        verify(userManagementService).sleep(1L);
        verify(userManagementService).activate(1L);
    }

    @Test
    @DisplayName("updateUserRoles - MEMBER 를 ADMIN 으로 승격 (MEMBER 제거 + ADMIN 추가)")
    void updateUserRoles_promote() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.MEMBER)));

        adminUserService.updateUserRoles(1L, List.of(Role.ADMIN));

        verify(userRoleService).addRole(user, Role.ADMIN);
        verify(userRoleService).removeRole(user, Role.MEMBER);
        verify(tokenBlacklistService).blacklistByUserId(1L);
    }

    @Test
    @DisplayName("updateUserRoles - ADMIN 을 MEMBER 로 강등")
    void updateUserRoles_demote() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.ADMIN)));

        adminUserService.updateUserRoles(1L, List.of(Role.MEMBER));

        verify(userRoleService).addRole(user, Role.MEMBER);
        verify(userRoleService).removeRole(user, Role.ADMIN);
        verify(tokenBlacklistService).blacklistByUserId(1L);
    }

    @Test
    @DisplayName("updateUserRoles - 현재와 동일하면 아무것도 안 함")
    void updateUserRoles_noop() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.MEMBER)));

        adminUserService.updateUserRoles(1L, List.of(Role.MEMBER));

        verify(userRoleService, never()).addRole(eq(user), any());
        verify(userRoleService, never()).removeRole(eq(user), any());
        verify(tokenBlacklistService, never()).blacklistByUserId(any());
    }

    @Test
    @DisplayName("updateUserRoles - 빈 목록이면 예외")
    void updateUserRoles_empty() {
        List<Role> emptyRoles = List.of();

        assertThatThrownBy(() -> adminUserService.updateUserRoles(1L, emptyRoles))
                .isInstanceOf(InvalidUserRoleException.class);
    }

    @Test
    @DisplayName("updateUserRoles - ADMIN 은 다른 권한과 함께 지정 불가")
    void updateUserRoles_adminNotCombinable() {
        List<Role> combinedRoles = List.of(Role.MEMBER, Role.ADMIN);

        assertThatThrownBy(() -> adminUserService.updateUserRoles(1L, combinedRoles))
                .isInstanceOf(InvalidUserRoleException.class);
    }

    @Test
    @DisplayName("강제 로그아웃은 UserAuthenticationService에 위임")
    void forceLogout_delegates() {
        adminUserService.forceLogout(1L);

        verify(userAuthenticationService).forceLogout(1L);
    }
}
