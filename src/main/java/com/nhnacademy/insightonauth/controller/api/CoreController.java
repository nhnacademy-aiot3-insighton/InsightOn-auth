package com.nhnacademy.insightonauth.controller.api;

import com.nhnacademy.insightonauth.controller.swagger.CoreApi;
import com.nhnacademy.insightonauth.dto.core.AuthUserResponse;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class CoreController implements CoreApi {

    private final UserManagementService userManagementService;

    @Override
    @GetMapping("/{user-id}")
    public ResponseEntity<AuthUserResponse> getUserById(@PathVariable("user-id") Long userId) {
        User user = userManagementService.findById(userId);

        return ResponseEntity.ok(
                new AuthUserResponse(user.getUserId(), user.getUserName(), user.getPhoneNumber(), user.getStatus()));
    }

    @Override
    @GetMapping("/invite/{user-email}")
    public ResponseEntity<AuthUserResponse> getUserByEmail(@PathVariable("user-email") String userEmail) {
        User user = userManagementService.findByEmail(userEmail);

        return ResponseEntity.ok(
                new AuthUserResponse(user.getUserId(), user.getUserName(), user.getPhoneNumber(), user.getStatus()));
    }
}
