package com.taskcenter.exception;

/**
 * Resurs ziddiyati (dublikat) → 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
