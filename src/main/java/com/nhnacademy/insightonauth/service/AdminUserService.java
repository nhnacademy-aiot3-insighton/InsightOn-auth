package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.admin.RoleResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminUserService {

    Page<AdminFindUsersResponse> findUsers(
            String email, String userName, Status status, Pageable pageable);

    AdminUserDetailResponse findUserDetail(Long userId);

    void block(Long userId);

    void sleep(Long userId);

    void activate(Long userId);

    void updateUserRoles(Long userId, List<Role> roleList);

    List<RoleResponse> findAssignableRoles();

    void forceLogout(Long userId);
}
