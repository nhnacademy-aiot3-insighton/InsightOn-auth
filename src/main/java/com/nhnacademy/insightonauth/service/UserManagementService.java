package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserManagementService {
    UserSignupResponse createUser(String email, String password, String userName,
                                  String phoneNumber, Role role, String verificationToken);

    boolean checkEmailAvailable(String email);

    User findById(Long userId);

    User findByEmail(String email);

    /**
     * 활성 계정을 email 로 찾고, 없으면 복구 기간 내 탈퇴 계정을 마스킹 접두어로 찾는다.
     * 로그인/복구 진입점에서 사용.
     */
    Optional<User> findReactivatableByEmail(String email);

    void reactivate(User user);

    void activate(Long userId);

    void withdraw(Long userId, String accessToken);

    void sleep(Long userId);

    void block(Long userId);

    void deleteUser(Long userId);

    void updateUserName(Long userId, String newUserName);

    void updatePhoneNumber(Long userId, String phoneNumber);

    String findMaskedEmail(String userName, String phoneNumber);

    List<User> findExpiredWithdrawnUsers();

    List<User> findInactiveUsers();
}
