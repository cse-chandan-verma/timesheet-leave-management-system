package com.application.timesheet.messaging;

import com.application.timesheet.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetEventPublisher — Unit Tests")
class TimesheetEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TimesheetEventPublisher publisher;

    @Test
    @DisplayName("publishTimesheetSubmitted() — success")
    void publishTimesheetSubmitted_Success() {
        publisher.publishTimesheetSubmitted(1L, "test@test.com", LocalDate.now(), 40.0);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.TIMESHEET_SUBMITTED_ROUTING_KEY),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("publishTimesheetStatusUpdated() — success")
    void publishTimesheetStatusUpdated_Success() {
        publisher.publishTimesheetStatusUpdated("test@test.com", "John", "APPROVED", "Good", LocalDate.now());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("timesheet.status.updated"),
                any(Map.class)
        );
    }
}
