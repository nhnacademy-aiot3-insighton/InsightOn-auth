package com.nhnacademy.insightonauth.redis;

public enum RedisKey {
    // 리프레시 토큰 (userId → jti). 재발급/로그아웃 시 검증에 사용
    REFRESH("refresh:"),

    // 현재 유효한 액세스 토큰 (userId → jti, accessValidity).
    // 강제 로그아웃·계정 정지·동시 로그인 축출 시 이 jti 를 블랙리스트에 올린다.
    ACCESS_JTI("access-jti:"),

    // 이메일 인증 최종 완료 토큰 (email → token, 15분). 회원가입 시 인증 여부 확인
    VERIFIED("verified:"),

    // 이메일 인증 코드 (email → 6자리 코드, 5분)
    VERIFY("verify:"),

    // 이메일 인증 코드 입력 실패 횟수 (email → count, 5분). 5회 초과 시 잠금
    VERIFY_FAIL("verify-fail:"),

    // 이메일 인증 실패 잠금 (email → "locked", 5분). 존재 시 인증 시도 차단
    VERIFY_FAIL_LOCK("verify-fail-lock:"),

    // 인증 코드 재전송 연타 방지 쿨다운 (email, 예: 60초). 존재 시 재전송 거부
    VERIFY_RESEND_COOLDOWN("verify-resend-cooldown:"),

    // 인증 코드 재전송 누적 횟수 (email → count). 임계치 초과 시 잠금
    VERIFY_RESEND_COUNT("verify-resend-count:"),

    // 인증 코드 재전송 잠금 (email → "locked"). 존재 시 재전송 차단
    VERIFY_RESEND_LOCK("verify-resend-lock:"),

    // 로그인 실패 횟수 (email → count). 임계치 초과 시 잠금
    LOGIN_FAIL("login-fail:"),

    // 로그인 실패 잠금 (email → "locked"). 존재 시 로그인 차단
    LOGIN_LOCK("login-lock:"),

    // 비밀번호 재설정 토큰 (uuid → email, 10분). 재설정 경로 접근 시 검증
    PASSWORD_RESET("password-reset:"),

    // 비밀번호 재설정 역방향 키 (email → uuid, 10분). 재전송 시 예전 토큰 무효화용
    PASSWORD_RESET_BY_EMAIL("password-reset-by-email:"),

    // 비밀번호 재설정 메일 재전송 연타 방지 (email, 예: 60초)
    PASSWORD_RESET_RESEND_COOLDOWN("password-reset-resend-cooldown:"),

    // 비밀번호 재설정 메일 재전송 누적 횟수 (email → count)
    PASSWORD_RESET_RESEND_COUNT("password-reset-resend-count:"),

    // 비밀번호 재설정 메일 재전송 잠금 (email → "locked")
    PASSWORD_RESET_RESEND_LOCK("password-reset-resend-lock:"),

    // 재활성화 인증 코드 (email → 6자리 코드, 5분)
    REACTIVE("reactive:"),

    // 재활성화 인증 코드 입력 실패 횟수 (email → count, 5분). 5회 초과 시 잠금
    REACTIVE_VERIFY_FAIL("reactive-fail-count:"),

    // 재활성화 인증 실패 잠금 (email → "locked", 5분). 존재 시 인증 시도 차단
    REACTIVE_VERIFY_FAIL_LOCK("reactive-fail-lock:"),

    // 재활성화 코드 재전송 연타 방지 쿨다운 (email, 예: 60초)
    REACTIVE_RESEND_COOLDOWN("reactive-resend-cooldown:"),

    // 재활성화 코드 재전송 누적 횟수 (email → count)
    REACTIVE_RESEND_COUNT("reactive-resend-count:"),

    // 재활성화 코드 재전송 잠금 (email → "locked")
    REACTIVE_RESEND_LOCK("reactive-resend-lock:"),

    // 무효화된 액세스 토큰 블랙리스트 (로그아웃/차단 토큰)
    BLACKLIST("blacklist:"),

    // 탈퇴 계정 하드 삭제 스케줄러 분산 락 (Redisson RLock)
    HARD_DELETE_SCHEDULER_LOCK("scheduler-lock:hard-delete-users"),

    // 휴면 전환 스케줄러 분산 락 (Redisson RLock)
    SLEEP_CONVERSION_SCHEDULER_LOCK("scheduler-lock:sleep-conversion");

    private final String prefix;

    RedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}