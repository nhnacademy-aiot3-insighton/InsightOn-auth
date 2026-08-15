package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.RoleResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;

public interface UserService {

    UserSignupResponse createUser(String email, String password, String userName, String phoneNumber, Role role, String verificationToken);

    boolean checkEmailAvailable(String email);

    void emailVerifyRequest(String email);

    String emailVerifyConfirm(String email, String code);

    User findById(Long userId);

    User findByEmail(String email);

    UserLoginResponse login(String email, String password);

    void logout(Long userId, String accessToken);

    void forceLogout(Long userId);

    void reactivateRequest(String email);

    UserLoginResponse reactivateConfirm(String email, String code);

    UserLoginResponse reactive(String restoreToken);

    void passwordResetRequest(String email);

    void passwordResetConfirm(String token, String newPassword);

    void updateUserName(Long userId, String newUserName);

    void updatePhoneNumber(Long userId, String phoneNumber);

    String findMaskedEmail(String userName, String phoneNumber);

    void activate(Long userId);

    void withdraw(Long userId, String accessToken);

    void sleep(Long userId);

    void block(Long userId);

    void deleteUser(Long userId);

    UserLoginResponse oauthLogin(String provider, String code);

    TokenRefreshResponse refresh(Long userId, String refreshToken);

    List<User> findExpiredWithdrawnUsers();

    List<User> findInactiveUsers();
}
