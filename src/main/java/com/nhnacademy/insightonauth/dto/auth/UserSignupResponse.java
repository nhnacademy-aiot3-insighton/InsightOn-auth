package com.nhnacademy.insightonauth.dto.auth;

import java.time.OffsetDateTime;

public record UserSignupResponse (
        String email,
        String userName,
        String phoneNumber,
        OffsetDateTime createdAt
){
}
