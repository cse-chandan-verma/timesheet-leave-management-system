package com.application.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    @Test
    @DisplayName("handleRuntime() — returns 400")
    void handleRuntime_Returns400() {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        RuntimeException ex = new RuntimeException("User already exists");

        ResponseEntity<Map<String, Object>> resp = handler.handleRuntime(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("message", "User already exists");
    }

    @Test
    @DisplayName("handleBadCredentials() — returns 401")
    void handleBadCredentials_Returns401() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        BadCredentialsException ex = new BadCredentialsException("Invalid password");

        ResponseEntity<Map<String, Object>> resp = handler.handleBadCredentials(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody())
                .containsKey("message")
                .extractingByKey("message").asString().contains("Invalid email or password");
    }

    @Test
    @DisplayName("handleDisabled() — returns 401")
    void handleDisabled_Returns401() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        DisabledException ex = new DisabledException("Disabled");

        ResponseEntity<Map<String, Object>> resp = handler.handleDisabled(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody())
                .containsKey("message")
                .extractingByKey("message").asString().contains("contact HR");
    }

    @Test
    @DisplayName("handleAll() — returns 500")
    void handleAll_Returns500() {
        when(request.getRequestURI()).thenReturn("/api/auth/reset");
        Exception ex = new Exception("Critical error");

        ResponseEntity<Map<String, Object>> resp = handler.handleAll(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsEntry("message", "An unexpected error occurred");
    }
}
