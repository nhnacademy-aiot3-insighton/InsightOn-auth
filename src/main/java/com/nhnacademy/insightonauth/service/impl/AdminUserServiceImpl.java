package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.AdminUserService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final UserManagementService userManagementService;

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


    @Override
    public void addUserRole(Long userId, Role role) {
        User user = userManagementService.findById(userId);
        userRoleService.addRole(user, role);
    }

    @Override
    public void removeUserRole(Long userId, Role role) {
        User user = userManagementService.findById(userId);
        userRoleService.removeRole(user, role);
    }

    @Override
    public void forceLogout(Long userId) {
        userService.forceLogout(userId);
    }

}
