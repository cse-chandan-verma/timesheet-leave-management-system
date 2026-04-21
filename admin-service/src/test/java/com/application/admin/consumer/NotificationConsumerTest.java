package com.application.admin.consumer;

import com.application.admin.entity.AdminNotification;
import com.application.admin.repository.AdminNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer — Unit Tests")
class NotificationConsumerTest {

    @Mock
    private AdminNotificationRepository repository;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    @DisplayName("handleTimesheetEvent() — success")
    void handleTimesheetEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("employeeEmail", "test@test.com");
        event.put("status", "SUBMITTED");

        consumer.handleTimesheetEvent(event);

        verify(repository).save(any(AdminNotification.class));
    }

    @Test
    @DisplayName("handleLeaveEvent() — success")
    void handleLeaveEvent_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("employeeEmail", "user@test.com");
        event.put("status", "SUBMITTED");

        consumer.handleLeaveEvent(event);

        verify(repository).save(any(AdminNotification.class));
    }
}
