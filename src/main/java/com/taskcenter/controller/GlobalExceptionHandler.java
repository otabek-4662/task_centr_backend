package com.taskcenter.controller;

import com.taskcenter.dto.ApiError;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.RateLimitException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    // 1. Validation: MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorItem> fieldErrors = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.add(new ApiError.FieldErrorItem(error.getField(), error.getDefaultMessage()));
            errors.put(error.getField(), error.getDefaultMessage());
        });

        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            fieldErrors.add(new ApiError.FieldErrorItem(error.getObjectName(), error.getDefaultMessage()));
            errors.put(error.getObjectName(), error.getDefaultMessage());
        });

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Validatsiya xatosi")
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .data(errors)
                .code("VALIDATION_ERROR")
                .errorCode("VALIDATION_ERROR")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 2. Validation: ConstraintViolationException (e.g. @PathVariable, @RequestParam)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorItem> fieldErrors = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String prop = violation.getPropertyPath().toString();
            String field = prop.contains(".") ? prop.substring(prop.lastIndexOf('.') + 1) : prop;
            fieldErrors.add(new ApiError.FieldErrorItem(field, violation.getMessage()));
            errors.put(field, violation.getMessage());
        });

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Validatsiya xatosi")
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .data(errors)
                .code("VALIDATION_ERROR")
                .errorCode("VALIDATION_ERROR")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 3. Validation: Custom ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleCustomValidationException(ValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getErrors() != null ? ex.getErrors() : Map.of("error", ex.getMessage());
        List<ApiError.FieldErrorItem> fieldErrors = new ArrayList<>();
        errors.forEach((field, msg) -> fieldErrors.add(new ApiError.FieldErrorItem(field, msg)));

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .data(errors)
                .code("VALIDATION_ERROR")
                .errorCode("VALIDATION_ERROR")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 4. 404: ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode() : "NOT_FOUND";
        ApiError body = ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("NOT_FOUND")
                .errorCode(errCode)
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 5. 404: UsernameNotFoundException
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("NOT_FOUND")
                .errorCode("NOT_FOUND")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 6. 404: NoResourceFoundException (Spring Web 404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message("Resurs topilmadi: " + ex.getResourcePath())
                .path(getPath(request))
                .code("NOT_FOUND")
                .errorCode("NOT_FOUND")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 6b. 404: NoSuchElementException
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Resurs topilmadi")
                .path(getPath(request))
                .code("NOT_FOUND")
                .errorCode("NOT_FOUND")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 7. 403: ForbiddenException
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode() : "FORBIDDEN";
        ApiError body = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("FORBIDDEN")
                .errorCode(errCode)
                .maskedReceiverEmail(ex.getMaskedReceiverEmail())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // 8. 403: AccessDeniedException
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Amalni bajarish uchun ruxsat yetarli emas";
        ApiError body = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(message)
                .path(getPath(request))
                .code("FORBIDDEN")
                .errorCode("FORBIDDEN")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // 9. 401: BadCredentialsException
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message("Nom yoki parol xato")
                .path(getPath(request))
                .code("UNAUTHORIZED")
                .errorCode("UNAUTHORIZED")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 10. 400: BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode() : "BAD_REQUEST";
        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("BAD_REQUEST")
                .errorCode(errCode)
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 11. 400: IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("BAD_REQUEST")
                .errorCode("BAD_REQUEST")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 12. 400: HttpMessageNotReadableException (JSON formati xato)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("JSON parse xatosi: path={}", getPath(request), ex);
        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Noto'g'ri so'rov formati (JSON xatosi)")
                .path(getPath(request))
                .code("BAD_REQUEST")
                .errorCode("BAD_REQUEST")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 12b. 400: MissingServletRequestParameterException
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorItem> fieldErrors = List.of(
                new ApiError.FieldErrorItem(ex.getParameterName(), "Parametr kiritilishi shart")
        );
        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Majburiy so'rov parametri yetishmaydi: " + ex.getParameterName())
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .code("BAD_REQUEST")
                .errorCode("BAD_REQUEST")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 12c. 400: MethodArgumentTypeMismatchException
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String name = ex.getName();
        String message = String.format("Parametr '%s' uchun noto'g'ri qiymat: '%s'", name, ex.getValue());
        List<ApiError.FieldErrorItem> fieldErrors = List.of(
                new ApiError.FieldErrorItem(name, message)
        );
        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(message)
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .code("BAD_REQUEST")
                .errorCode("BAD_REQUEST")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 12d. 405: HttpRequestMethodNotSupportedException
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_ALLOWED")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("METHOD_NOT_ALLOWED")
                .errorCode("METHOD_NOT_ALLOWED")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // 13. 409: ConflictException
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode() : "CONFLICT";
        ApiError body = ApiError.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("CONFLICT")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("CONFLICT")
                .errorCode(errCode)
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 14. 409: DataIntegrityViolationException
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: path={}", getPath(request), ex);
        ApiError body = ApiError.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("CONFLICT")
                .message("Ma'lumotlar bazasida xatolik (butunlik buzilishi yoki takrorlanish yuz berdi)")
                .path(getPath(request))
                .code("CONFLICT")
                .errorCode("CONFLICT")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 15. 409: ObjectOptimisticLockingFailureException
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("CONFLICT")
                .message("Ma'lumotlar bir vaqtda o'zgartirildi. Iltimos, qayta urinib ko'ring.")
                .path(getPath(request))
                .code("CONFLICT")
                .errorCode("CONFLICT")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 16. 429: RateLimitException
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimitException(RateLimitException ex, HttpServletRequest request) {
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode() : "TOO_MANY_REQUESTS";
        ApiError body = ApiError.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message(ex.getMessage())
                .path(getPath(request))
                .code("TOO_MANY_REQUESTS")
                .errorCode(errCode)
                .retryAfterSeconds(ex.getRetryAfterSeconds())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    // 17. 413: MaxUploadSizeExceededException
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error("PAYLOAD_TOO_LARGE")
                .message("Fayl hajmi 10 MB dan oshmasligi kerak")
                .path(getPath(request))
                .code("PAYLOAD_TOO_LARGE")
                .errorCode("PAYLOAD_TOO_LARGE")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    // 18. ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        int statusCode = ex.getStatusCode().value();
        HttpStatus resolved = HttpStatus.resolve(statusCode);
        String error = resolved != null ? resolved.name() : "ERROR";
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        ApiError body = ApiError.builder()
                .status(statusCode)
                .error(error)
                .message(message)
                .path(getPath(request))
                .code(error)
                .errorCode(error)
                .success(false)
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // 19. 500: DataAccessException
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("Ma'lumotlar bazasida xatolik yuz berdi: path={}", getPath(request), ex);
        ApiError body = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("Ma'lumotlar bazasida ichki xatolik yuz berdi")
                .path(getPath(request))
                .code("INTERNAL_ERROR")
                .errorCode("INTERNAL_ERROR")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // 20. 500: Noma'lum umumiy xato (stack trace faqat logda, javobda emas)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Kutilmagan ichki server xatoligi: path={}", getPath(request), ex);
        ApiError body = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("Kutilmagan ichki server xatoligi yuz berdi")
                .path(getPath(request))
                .code("INTERNAL_ERROR")
                .errorCode("INTERNAL_ERROR")
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
