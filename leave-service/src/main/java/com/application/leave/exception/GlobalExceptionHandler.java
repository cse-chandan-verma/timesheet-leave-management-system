package com.application.leave.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Bean Validation (@Valid) errors ──────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });

        Map<String, Object> response = buildError(
                HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        response.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    // ── Known business-rule violations → 400 Bad Request ────────────────────
    @ExceptionHandler(LeaveException.class)
    public ResponseEntity<Map<String, Object>> handleLeaveException(
            LeaveException ex, HttpServletRequest request) {

        log.warn("Leave business error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }

    // ── Authorization failures → 403 Forbidden ───────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildError(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.",
                        request.getRequestURI()));
    }

    // ── Anything unexpected → 500 Internal Server Error ─────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred", request.getRequestURI()));
    }

    private Map<String, Object> buildError(HttpStatus status, String message, String path) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", path);
        return error;
    }
}
