package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.RoleResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;

import java.util.List;

public interface MyPageService {

    MyInfoResponse findMyInfo(Long userId);

    void updatePassword(Long userId, String currentPassword, String newPassword);

    List<RoleResponse> findMyRoles(Long userId);

    List<OauthResponse> findMyOauths(Long userId);

    void linkOauth(Long userId, String provider, String code);
}
