package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.exception.UserRoleNotFoundException;
import com.nhnacademy.insightonauth.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "010123456789");
    }

    @Test
    @DisplayName("역할 생성됨")
    void create() {
        userRoleService.create(user, Role.MEMBER);

        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("이미 역할이 있으면 추가 없이 스킵됨")
    void addRole_alreadyExists() {
        when(userRoleRepository.existsByUserAndRole(user, Role.MEMBER)).thenReturn(true);

        userRoleService.addRole(user, Role.MEMBER);

        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(userRoleRepository, times(1)).existsByUserAndRole(user, Role.MEMBER);
    }

    @Test
    @DisplayName("역할이 없으면 추가됨")
    void addRole_notExists() {
        when(userRoleRepository.existsByUserAndRole(user, Role.ADMIN)).thenReturn(false);

        userRoleService.addRole(user, Role.ADMIN);

        verify(userRoleRepository, times(1)).save(any(UserRole.class));
        verify(userRoleRepository, times(1)).existsByUserAndRole(user, Role.ADMIN);
    }

    @Test
    @DisplayName("역할이 있으면 삭제됨")
    void removeRole_exists() {
        UserRole userRole = mock(UserRole.class);
        when(userRoleRepository.findByUserAndRole(user, Role.MEMBER)).thenReturn(Optional.of(userRole));

        userRoleService.removeRole(user, Role.MEMBER);

        verify(userRoleRepository, times(1)).delete(userRole);
    }

    @Test
    @DisplayName("역할이 없으면 예외 발생")
    void removeRole_notExists() {
        when(userRoleRepository.findByUserAndRole(user, Role.MEMBER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.removeRole(user, Role.MEMBER))
                .isInstanceOf(UserRoleNotFoundException.class)
                        .hasMessage("해당 권한이 존재하지 않습니다.");

        verify(userRoleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("사용자의 역할 목록 조회됨")
    void findByUser() {
        List<UserRole> userRoleList = List.of(new UserRole(user, Role.MEMBER));
        when(userRoleRepository.findByUser(user)).thenReturn(userRoleList);

        List<UserRole> result = userRoleService.findByUser(user);

        assertThat(result).isEqualTo(userRoleList);
    }

    @Test
    @DisplayName("역할이 없으면 빈 목록 반환")
    void findByUser_empty() {
        when(userRoleRepository.findByUser(user)).thenReturn(List.of());

        List<UserRole> result = userRoleService.findByUser(user);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자의 모든 역할 삭제됨")
    void deleteUserRole() {
        List<UserRole> userRoleList = List.of(new UserRole(user, Role.MEMBER));
        when(userRoleRepository.findByUser(user)).thenReturn(userRoleList);

        userRoleService.deleteUserRole(user);

        verify(userRoleRepository, times(1)).findByUser(any());
        verify(userRoleRepository, times(1)).deleteAll(userRoleList);
    }
}
