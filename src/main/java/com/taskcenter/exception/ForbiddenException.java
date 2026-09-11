package com.taskcenter.exception;

/**
 * Foydalanuvchi ruxsatsiz amalni bajarishga uringanda otiladi.
 * GlobalExceptionHandler bu exceptionni 403 FORBIDDEN ga aylantiradi.
 *
 * Misol:
 *   throw new ForbiddenException("Faqat workspace egasi o'chira oladi");
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    // Standart xabar bilan qulay konstruktor
    public ForbiddenException() {
        super("Bu amalni bajarish uchun ruxsat yo'q");
    }
}
