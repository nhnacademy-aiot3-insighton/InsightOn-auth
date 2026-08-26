package com.nhnacademy.insightonauth.dto.mypage;

import java.time.OffsetDateTime;

public record MyInfoResponse(
        String email,
        String userName,
        String phoneNumber,
        OffsetDateTime createdAt,
        String groupName
) {
}
