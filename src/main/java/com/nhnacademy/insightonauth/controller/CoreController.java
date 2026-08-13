package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.core.AuthUserResponse;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class CoreController {

    private final UserService userService;

    @GetMapping("{userId}")
    public ResponseEntity<AuthUserResponse> getUserById(@PathVariable("userId") Long userId) {
        User user = userService.findById(userId);

        return ResponseEntity.ok(
                new AuthUserResponse(user.getUserId(), user.getUserName(), user.getPhoneNumber(), user.getStatus()));
    }
}
