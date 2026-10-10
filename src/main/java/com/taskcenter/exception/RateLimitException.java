package com.taskcenter.exception;

public class RateLimitException extends RuntimeException {
    private final String errorCode;
    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        this("RATE_LIMITED", "Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.", retryAfterSeconds);
    }

    public RateLimitException(String message, long retryAfterSeconds) {
        this("RATE_LIMITED", message, retryAfterSeconds);
    }

    public RateLimitException(String errorCode, String message, long retryAfterSeconds) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "RATE_LIMITED";
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
