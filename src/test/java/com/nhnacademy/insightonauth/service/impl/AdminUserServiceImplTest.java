package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.repository.UserRepository;
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
    @DisplayName("역할 추가는 findById 후 addRole 호출")
    void addUserRole() {
        when(userManagementService.findById(1L)).thenReturn(user);

        adminUserService.addUserRole(1L, Role.ADMIN);

        verify(userRoleService).addRole(user, Role.ADMIN);
    }

    @Test
    @DisplayName("역할 제거는 findById 후 removeRole 호출")
    void removeUserRole() {
        when(userManagementService.findById(1L)).thenReturn(user);

        adminUserService.removeUserRole(1L, Role.ADMIN);

        verify(userRoleService).removeRole(user, Role.ADMIN);
    }

    @Test
    @DisplayName("강제 로그아웃은 UserAuthenticationService에 위임")
    void forceLogout_delegates() {
        adminUserService.forceLogout(1L);

        verify(userAuthenticationService).forceLogout(1L);
    }
}
