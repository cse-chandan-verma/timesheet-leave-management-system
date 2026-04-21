package com.application.leave.messaging;

import com.application.leave.config.RabbitMQConfig;
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
@DisplayName("LeaveEventPublisher — Unit Tests")
class LeaveEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LeaveEventPublisher publisher;

    @Test
    @DisplayName("publishLeaveApplied() — success")
    void publishLeaveApplied_Success() {
        publisher.publishLeaveApplied(1L, "test@test.com", "AL", LocalDate.now(), LocalDate.now().plusDays(1), 2, 100L);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.LEAVE_APPLIED_ROUTING_KEY),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("publishLeaveStatusUpdated() — success")
    void publishLeaveStatusUpdated_Success() {
        publisher.publishLeaveStatusUpdated("test@test.com", "John", "APPROVED", "Good", "AL", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("leave.status.approved"),
                any(Map.class)
        );
    }
}
