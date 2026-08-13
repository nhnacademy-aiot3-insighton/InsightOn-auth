package com.nhnacademy.insightonauth.exception;

public class EmailSendException extends BusinessException {
    public EmailSendException(String message, Exception e) {
        super(ErrorCode.EMAIL_SEND_FAILED, message);
        // 왜 이메일이 실패했는지 정확인 exception이 로그이 보이기 위한 코드
        initCause(e);
    }
}
