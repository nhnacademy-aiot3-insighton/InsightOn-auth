package com.nhnacademy.insightonauth.redis;

public enum RedisKey {
    REFRESH("refresh:"),
    VERIFIED("verified:"),
    VERIFY("verify:"),
    VERIFY_FAIL("verify-fail:"),
    VERIFY_FAIL_LOCK("verify-fail-lock:"),
    LOGIN_FAIL("login-fail:"),
    LOGIN_LOCK("login-lock:"),
    PASSWORD_RESET("password-reset:"),
    RESTORE("restore:"),
    BLACKLIST("blacklist:");

    private final String prefix;

    RedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}