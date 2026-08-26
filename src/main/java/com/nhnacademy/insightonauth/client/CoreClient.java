package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "insighton-core", url = "${gateway-route.core}")
public interface CoreClient {

    // 마이페이지시 내 그룹 확인
    @GetMapping("/internal/v1/users/{userId}/group")
    UserGroupResponse getUserGroup(@PathVariable Long userId);

    // 그룹 관리자인지 확인
    @GetMapping("/internal/v1/users/{userId}/manager-group")
    Boolean isGroupManager(@PathVariable Long userId);
}
