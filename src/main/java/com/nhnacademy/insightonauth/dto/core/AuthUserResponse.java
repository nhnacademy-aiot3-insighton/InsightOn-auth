package com.nhnacademy.insightonauth.dto.core;

import com.nhnacademy.insightonauth.entity.Status;

public record AuthUserResponse(
       Long userId,
       String userName,
       String userPhoneNumber,
       Status userStatus
) {
}
