package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.dto.core.ManagerGroupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "insighton-core")
public interface CoreClient {

    @GetMapping("/internal/v1/users/{userId}/manager-group")
    ManagerGroupResponse existsManagerGroup(@PathVariable Long userId);
}
