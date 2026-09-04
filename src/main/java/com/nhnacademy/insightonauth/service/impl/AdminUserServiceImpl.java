package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.admin.RoleResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.exception.user.InvalidUserRoleException;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.AdminUserService;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserAuthenticationService userAuthenticationService;
    private final UserRoleService userRoleService;
    private final UserManagementService userManagementService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFindUsersResponse> findUsers(
            String email, String userName, Status status, Pageable pageable) {

        String emailCond = (email != null) ? email : "";
        String userNameCond = (userName != null) ? userName : "";

        Page<User> users;
        if (status != null) {
            users = userRepository.findByEmailContainingAndUserNameContainingAndStatus(
                    emailCond, userNameCond, status, pageable);
        } else {
            users = userRepository.findByEmailContainingAndUserNameContaining(
                    emailCond, userNameCond, pageable);
        }

        return users.map(user -> new AdminFindUsersResponse(
                user.getUserId(), user.getEmail(), user.getUserName(),
                user.getStatus(), user.getCreatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse findUserDetail(Long userId) {
        User user = userManagementService.findById(userId);
        List<UserRole> userRoleList = userRoleService.findByUser(user);

        List<Role> roles = userRoleList.stream()
                .map(UserRole::getRole)
                .toList();

        return new AdminUserDetailResponse(
                user.getUserId(), user.getEmail(), user.getUserName(), user.getStatus(), roles);
    }

    @Override
    public void block(Long userId) {
        userManagementService.block(userId);
    }

    @Override
    public void sleep(Long userId) {
        userManagementService.sleep(userId);
    }

    @Override
    public void activate(Long userId) {
        userManagementService.activate(userId);
    }

    /**
     * 회원 권한을 {@code roleList} 로 전체 교체한다.
     * 기존에 있고 목록에도 있으면 유지, 목록에만 있으면 추가, 목록에 없으면 제거.
     * 권한 셋이 바뀌면 현재 access 토큰을 무효화해 재로그인 시 새 권한이 반영되게 한다.
     */
    @Override
    public void updateUserRoles(Long userId, List<Role> roleList) {
        if (roleList == null || roleList.isEmpty()) {
            throw new InvalidUserRoleException("권한은 최소 1개 이상이어야 합니다.");
        }

        Set<Role> target = EnumSet.copyOf(roleList);
        // ADMIN 은 단독으로만 가진다 (회원 아니면 관리자).
        if (target.contains(Role.ADMIN) && target.size() > 1) {
            throw new InvalidUserRoleException("ADMIN 권한은 다른 권한과 함께 가질 수 없습니다.");
        }

        User user = userManagementService.findById(userId);

        Set<Role> current = userRoleService.findByUser(user).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));

        if (current.equals(target)) {
            return;   // 변화 없음
        }

        target.stream()
                .filter(role -> !current.contains(role))
                .forEach(role -> userRoleService.addRole(user, role));

        current.stream()
                .filter(role -> !target.contains(role))
                .forEach(role -> userRoleService.removeRole(user, role));

        tokenBlacklistService.blacklistByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAssignableRoles() {
        return Arrays.stream(Role.values())
                .map(RoleResponse::from)
                .toList();
    }

    @Override
    public void forceLogout(Long userId) {
        userAuthenticationService.forceLogout(userId);
    }

}
