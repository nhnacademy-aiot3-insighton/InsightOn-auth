package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.dto.core.ManagerGroupExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "insighton-core", url = "lb://insighton-core")
public interface CoreClient {

    @GetMapping("/internal/v1/users/{userId}/manager-groups/exists")
    ManagerGroupExistsResponse existsManagerGroup(@PathVariable Long userId);
}
