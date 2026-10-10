package com.taskcenter.exception;

public class BadRequestException extends RuntimeException {
    private final String errorCode;

    public BadRequestException(String message) {
        this("BAD_REQUEST", message);
    }

    public BadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "BAD_REQUEST";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
