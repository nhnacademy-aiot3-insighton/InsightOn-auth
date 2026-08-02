package com.nhnacademy.insightonauth.dto;

import com.nhnacademy.insightonauth.entity.Status;

import java.time.OffsetDateTime;

public record AdminFindUsersResponse(
        Long userId,
        String email,
        String userName,
        Status status,
        OffsetDateTime createdAt
) {
}