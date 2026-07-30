package com.nhnacademy.insightonauth.exception;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Exception e) {
        super(message);
    }
}
