package com.nhnacademy.insightonauth.dto.admin;

import com.nhnacademy.insightonauth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "변경할 권한은 필수입니다.")
        Role role
) {
}
