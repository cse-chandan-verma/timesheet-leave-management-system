package com.application.timesheet.messaging;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.application.timesheet.config.RabbitMQConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimesheetEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTimesheetSubmitted(Long employeeId,
                                           String employeeEmail,
                                           LocalDate weekStart,
                                           double totalHours) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",     "TIMESHEET_SUBMITTED");
            event.put("employeeId",    employeeId);
            event.put("employeeEmail", employeeEmail);
            event.put("weekStartDate", weekStart.toString());
            event.put("totalHours",    totalHours);
            event.put("status",        "SUBMITTED");
            event.put("message",       String.format("Timesheet submitted for week %s (%.1f hrs)", weekStart, totalHours));
            event.put("timestamp",     LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.TIMESHEET_SUBMITTED_ROUTING_KEY,
                    event);

            log.info("Published TIMESHEET_SUBMITTED for employee {}", employeeEmail);
        } catch (Exception e) {
            log.error("Failed to publish timesheet event: {}", e.getMessage());
        }
    }

    public void publishTimesheetStatusUpdated(String email,
                                               String fullName,
                                               String status,
                                               String remarks,
                                               LocalDate weekStart) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("email",     email);
            event.put("fullName",  fullName);
            event.put("status",    status);
            event.put("remarks",   remarks);
            event.put("weekStart", weekStart.toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "timesheet.status.updated",
                    event);

            log.info("Published timesheet.status.updated event for: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish timesheet status update: {}", e.getMessage());
        }
    }
}
