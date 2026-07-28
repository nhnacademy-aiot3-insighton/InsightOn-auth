package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.UserLoginRequest;
import com.nhnacademy.insightonauth.dto.UserSignupRequest;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public void doLogin(@RequestBody @Valid UserLoginRequest userLoginRequest) {
        userService.login(userLoginRequest.email(), userLoginRequest.password());
    }

    @PostMapping("/signup")
    public void doSignup(@RequestBody @Valid UserSignupRequest userSignupRequest) {
        userService.createUser(userSignupRequest.email(), userSignupRequest.password(), userSignupRequest.userName(), userSignupRequest.phoneNumber(), Role.MEMBER);
    }
}
