package com.nhnacademy.insightonauth.dto.admin;

import com.nhnacademy.insightonauth.entity.Role;

/**
 * 지정 가능한 회원 권한 하나. front 의 {@code adapter.admin.dto.RoleResponse} 와 대응.
 */
public record RoleResponse(
        String name,
        String label,
        boolean exclusive,
        boolean base
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.name(), role.getLabel(), role.isExclusive(), role.isBase());
    }
}
