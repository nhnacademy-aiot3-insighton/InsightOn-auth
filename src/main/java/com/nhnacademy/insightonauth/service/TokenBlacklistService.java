package com.nhnacademy.insightonauth.service;

public interface TokenBlacklistService {
    boolean isBlacklisted(String jti);

    void blacklistToken(String accessToken);

    /**
     * userId 로 현재 유효한 access 토큰(jti)을 찾아 블랙리스트에 올린다.
     * 대상의 토큰 문자열이 없는 관리자 액션(강제 로그아웃/정지/휴면)에서 사용한다.
     */
    void blacklistByUserId(Long userId);
}
