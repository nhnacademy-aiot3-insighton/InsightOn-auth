package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.entity.User;

public interface TokenService {
    /**
     * 로그인 성공 시 액세스/리프레시 토큰을 발급하고,
     * 기존 로그인 실패 기록(잠금/실패 카운트)을 정리한다.
     */
    UserLoginResult issueTokens(User user, String email);

    /**
     * 탈퇴 후 7일 이내(복구 가능 기간)인지 확인한다.
     */
    boolean isWithinRestorePeriod(User user);
}
