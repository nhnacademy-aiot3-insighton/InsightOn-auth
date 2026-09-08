package com.nhnacademy.insightonauth.provider;

/**
 * JWT 서명 키(PEM) 로딩에 실패했을 때 던진다. 애플리케이션 부팅 시점의 설정 오류라
 * HTTP 응답으로 내려갈 일이 없으므로 BusinessException 체계에 속하지 않는다.
 */
public class JwtKeyLoadException extends RuntimeException {
    public JwtKeyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
