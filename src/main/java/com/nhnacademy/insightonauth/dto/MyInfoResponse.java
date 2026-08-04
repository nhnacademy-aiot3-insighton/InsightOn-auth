package com.nhnacademy.insightonauth.dto;

import java.time.OffsetDateTime;

public record MyInfoResponse(
        String email,
        String userName,
        String phoneNumber,
        OffsetDateTime createdAt
) {
}
