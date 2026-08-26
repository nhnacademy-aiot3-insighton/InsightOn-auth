package com.nhnacademy.insightonauth.dto.core;

public record UserGroupResponse(
        boolean exists,
        String groupName
) {
}
