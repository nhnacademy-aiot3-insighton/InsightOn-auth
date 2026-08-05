package com.nhnacademy.insightonauth.dto.admin;

import com.nhnacademy.insightonauth.entity.Status;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull(message = "변경할 상태는 필수입니다.")
        Status status
) {
}
