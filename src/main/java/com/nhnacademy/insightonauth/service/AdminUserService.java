package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminFindUsersResponse> findUsers(
            String email, String userName, Status status, Pageable pageable);

    AdminUserDetailResponse findUserDetail(Long userId);

    void changeStatus(Long userId, Status status);

    void addUserRole(Long userId, Role role);

    void removeUserRole(Long userId, Role role);

    void forceLogout(Long userId);

    void deleteUser(Long userId);
}
