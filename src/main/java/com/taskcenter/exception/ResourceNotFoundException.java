package com.taskcenter.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String errorCode;

    public ResourceNotFoundException(String message) {
        this("NOT_FOUND", message);
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
