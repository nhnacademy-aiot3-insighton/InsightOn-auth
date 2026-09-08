package com.nhnacademy.insightonauth.dto.admin;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;

import java.util.List;

public record AdminUserDetailResponse(
        Long userId,
        String email,
        String userName,
        Status status,
        List<Role> roles
) {
}
