package com.application.leave.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    @Test
    @DisplayName("handleLeaveException() — returns 400")
    void handleLeaveException_Returns400() {
        when(request.getRequestURI()).thenReturn("/api/leave/apply");
        LeaveException ex = new LeaveException("Invalid dates");

        ResponseEntity<Map<String, Object>> resp = handler.handleLeaveException(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody())
                .containsEntry("message", "Invalid dates")
                .containsEntry("path", "/api/leave/apply");
    }

    @Test
    @DisplayName("handleAccessDenied() — returns 403")
    void handleAccessDenied_Returns403() {
        when(request.getRequestURI()).thenReturn("/api/leave/approve");
        AccessDeniedException ex = new AccessDeniedException("Denied");

        ResponseEntity<Map<String, Object>> resp = handler.handleAccessDenied(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody())
                .containsKey("message")
                .extractingByKey("message").asString().contains("permission");
    }

    @Test
    @DisplayName("handleAll() — returns 500")
    void handleAll_Returns500() {
        when(request.getRequestURI()).thenReturn("/api/leave/123");
        Exception ex = new Exception("Critical failure");

        ResponseEntity<Map<String, Object>> resp = handler.handleAll(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsEntry("message", "An unexpected error occurred");
    }
}
