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
    REACTIVE("reactive:"),
    BLACKLIST("blacklist:"),
    HARD_DELETE_SCHEDULER_LOCK("scheduler-lock:hard-delete-users"),
    SLEEP_CONVERSION_SCHEDULER_LOCK("scheduler-lock:sleep-conversion");;

    private final String prefix;

    RedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}