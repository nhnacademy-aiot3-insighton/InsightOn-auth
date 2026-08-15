package com.nhnacademy.insightonauth.service;

public interface TokenBlacklistService {
    boolean isBlacklisted(String jti);

    void blacklistToken(String accessToken);
}
