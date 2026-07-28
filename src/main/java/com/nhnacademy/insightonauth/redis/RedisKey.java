package com.nhnacademy.insightonauth.redis;

public enum RedisKey {
    REFRESH("refresh:"),
    VERIFY("verify:"),
    LOGIN_FAIL("login-fail:"),
    BLACKLIST("blacklist:");

    private final String prefix;

    RedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}