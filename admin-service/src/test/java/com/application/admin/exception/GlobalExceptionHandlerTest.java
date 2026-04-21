package com.application.admin.exception;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleAdminException() — returns 400")
    void handleAdminException_Returns400() {
        AdminException ex = new AdminException("Bad request");
        ResponseEntity<Map<String, Object>> resp = handler.handleAdminException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("message", "Bad request");
    }

    @Test
    @DisplayName("handleGeneralException() — returns 500")
    void handleGeneralException_Returns500() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneralException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody())
                .containsKey("message")
                .extractingByKey("message").asString().contains("Unexpected error");
    }

    @Test
    @DisplayName("handleFeignException() — returns same status as downstream")
    void handleFeignException_ReturnsStatus() {
        FeignException ex = Mockito.mock(FeignException.class);
        Mockito.when(ex.status()).thenReturn(404);
        Mockito.when(ex.contentUTF8()).thenReturn("{}");

        ResponseEntity<String> resp = handler.handleFeignException(ex);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }
}
