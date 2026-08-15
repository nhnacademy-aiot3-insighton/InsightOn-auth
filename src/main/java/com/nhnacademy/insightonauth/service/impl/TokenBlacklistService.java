package com.nhnacademy.insightonauth.service.impl;

public interface TokenBlacklistService {
    boolean isBlacklisted(String jti);

    void blacklistToken(String accessToken);
}
