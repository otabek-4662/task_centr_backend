package com.taskcenter.exception;

public class ForbiddenException extends RuntimeException {
    private final String errorCode;
    private final String maskedReceiverEmail;

    public ForbiddenException(String message) {
        this("FORBIDDEN", message, null);
    }

    public ForbiddenException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ForbiddenException(String errorCode, String message, String maskedReceiverEmail) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "FORBIDDEN";
        this.maskedReceiverEmail = maskedReceiverEmail;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMaskedReceiverEmail() {
        return maskedReceiverEmail;
    }
}
