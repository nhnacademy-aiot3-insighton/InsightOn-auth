package com.nhnacademy.insightonauth.redis;

public enum RedisKey {
    REFRESH("refresh:"),
    VERIFIED("verified:"),
    VERIFY("verify:"),
    LOGIN_FAIL("login-fail:"),
    LOGIN_LOCK("login-lock:"),
    PASSWORD_RESET("password-reset:"),
    BLACKLIST("blacklist:");

    private final String prefix;

    RedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}