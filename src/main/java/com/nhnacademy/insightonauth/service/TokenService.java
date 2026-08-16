package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.entity.User;

public interface TokenService {
    /**
     * 로그인 성공 시 액세스/리프레시 토큰을 발급하고,
     * 기존 로그인 실패 기록(잠금/실패 카운트)을 정리한다.
     */
    UserLoginResponse issueTokens(User user, String email);

    /**
     * 탈퇴 후 7일 이내(복구 가능 기간)인지 확인한다.
     */
    boolean isWithinRestorePeriod(User user);

    /**
     * 탈퇴 복구 가능 기간 내 로그인 시, 복구용 토큰을 발급해
     * 즉시 로그인 대신 복구 확인 절차로 안내한다.
     */
    UserLoginResponse handleWithdrawnLogin(User user);
}
