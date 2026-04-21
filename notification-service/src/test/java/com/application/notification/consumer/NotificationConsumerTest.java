package com.application.notification.consumer;

import com.application.notification.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer — Unit Tests")
class NotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    @DisplayName("consumeRegistrationEvent() — success")
    void consumeRegistrationEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");
        event.put("fullName", "John");

        consumer.consumeRegistrationEvent(event);

        verify(emailService).sendRegistrationEmail(eq("test@test.com"), eq("John"));
    }

    @Test
    @DisplayName("consumeProfileUpdateEvent() — success")
    void consumeProfileUpdateEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");
        event.put("fullName", "John");

        consumer.consumeProfileUpdateEvent(event);

        verify(emailService).sendProfileUpdateEmail(eq("test@test.com"), eq("John"));
    }

    @Test
    @DisplayName("consumePasswordChangeEvent() — success")
    void consumePasswordChangeEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");

        consumer.consumePasswordChangeEvent(event);

        verify(emailService).sendPasswordChangeEmail(eq("test@test.com"));
    }

    @Test
    @DisplayName("consumeLeaveUpdateEvent() — success")
    void consumeLeaveUpdateEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");
        event.put("fullName", "John");
        event.put("status", "APPROVED");
        event.put("remarks", "Enjoy");
        event.put("leaveType", "AL");
        event.put("fromDate", "2024-01-01");
        event.put("toDate", "2024-01-02");

        consumer.consumeLeaveUpdateEvent(event);

        verify(emailService).sendLeaveStatusEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("consumeTimesheetUpdateEvent() — success")
    void consumeTimesheetUpdateEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");
        event.put("fullName", "John");
        event.put("status", "APPROVED");
        event.put("remarks", "Good");
        event.put("weekStart", "2024-01-01");

        consumer.consumeTimesheetUpdateEvent(event);

        verify(emailService).sendTimesheetStatusEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("consumeRoleUpdateEvent() — success")
    void consumeRoleUpdateEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("email", "test@test.com");
        event.put("fullName", "John");
        event.put("oldRole", "EMPLOYEE");
        event.put("newRole", "MANAGER");

        consumer.consumeRoleUpdateEvent(event);

        verify(emailService).sendRoleUpdateEmail(eq("test@test.com"), eq("John"), eq("EMPLOYEE"), eq("MANAGER"));
    }
}
