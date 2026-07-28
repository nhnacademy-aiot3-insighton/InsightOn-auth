package com.nhnacademy.insightonauth.dto;

import java.time.OffsetDateTime;

public record UserSignupResponse (
        String email,
        String userName,
        String phoneNumber,
        OffsetDateTime createdAt
){
}
