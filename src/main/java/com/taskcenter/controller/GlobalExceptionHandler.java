package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Barcha exceptionlarni markazlashgan holda ushlaydi va foydalanuvchiga
 * tushunarli xabar qaytaradi. Stack trace va ichki xabarlar oshkor qilinmaydi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // System.err.println o'rniga Logger ishlatamiz
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Biznes logika xatolari ────────────────────────────────────────────────

    /**
     * EntityNotFoundException: resurs topilmadi → 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * ForbiddenException: ruxsat yo'q → 403
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Spring Security xatolari ──────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Sizga ruxsat yo'q"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Nom yoki parol xato"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Validatsiya xatolari ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validatsiya xatosi"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Database xatolari ─────────────────────────────────────────────────────

    /**
     * DataIntegrityViolationException: dublikat yoki foreign key xatosi → 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Ma'lumotlar bazasida xatolik: dublikat yoki bog'lanish xatosi"));
    }

    /**
     * DataAccessException: ichki DB xatosi → 500.
     * Ichki xabar (rootCause) foydalanuvchiga ko'rsatilmaydi — faqat logga yoziladi.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        // Ichki xabarni faqat server logiga yozamiz
        log.error("Database xatolik: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Serverda xatolik yuz berdi. Iltimos, keyinroq urinib ko'ring."));
    }

    // ── Kutilmagan xatolik ────────────────────────────────────────────────────

    /**
     * Boshqa barcha RuntimeException lar → 500.
     * Xabar foydalanuvchiga oshkor qilinmaydi.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("Kutilmagan xatolik: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Serverda xatolik yuz berdi. Iltimos, keyinroq urinib ko'ring."));
    }
}
