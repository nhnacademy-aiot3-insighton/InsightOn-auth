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

    // 강제 삭제시 유저가 group 관리자인 경우 생각
    // group 관리를 넣는게 어떤가
    void deleteUser(Long userId);
}
