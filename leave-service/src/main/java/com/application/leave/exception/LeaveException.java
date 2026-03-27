package com.application.leave.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for all business-rule violations in the Leave Service.
 * Using a specific exception (rather than RuntimeException) ensures the
 * GlobalExceptionHandler returns a proper 400 — without accidentally
 * swallowing unexpected 500-level errors.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LeaveException extends RuntimeException {
    public LeaveException(String message) {
        super(message);
    }
}
