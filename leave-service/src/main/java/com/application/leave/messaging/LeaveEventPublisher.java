package com.application.leave.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.application.leave.config.RabbitMQConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishLeaveApplied(Long employeeId,
                                     String employeeEmail,
                                     String leaveType,
                                     LocalDate fromDate,
                                     LocalDate toDate,
                                     int totalDays,
                                     Long leaveRequestId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",      "LEAVE_APPLIED");
            event.put("leaveRequestId", leaveRequestId);
            event.put("employeeId",     employeeId);
            event.put("employeeEmail",   employeeEmail);
            event.put("leaveType",      leaveType);
            event.put("fromDate",       fromDate.toString());
            event.put("toDate",         toDate.toString());
            event.put("totalDays",      totalDays);
            event.put("status",         "SUBMITTED");
            event.put("message",        String.format("Leave applied (%s): %s to %s (%d days)", leaveType, fromDate, toDate, totalDays));
            event.put("timestamp",      LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.LEAVE_APPLIED_ROUTING_KEY,
                    event);

            log.info("Published LEAVE_APPLIED event for employee {}", employeeEmail);

        } catch (Exception e) {
            log.error("Failed to publish leave event: {}", e.getMessage());
        }
    }

    public void publishLeaveStatusUpdated(String email,
                                           String fullName,
                                           String status,
                                           String remarks,
                                           String leaveType,
                                           LocalDate fromDate,
                                           LocalDate toDate) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("email",     email);
            event.put("fullName",  fullName);
            event.put("status",    status);
            event.put("remarks",   remarks);
            event.put("leaveType", leaveType);
            event.put("fromDate",  fromDate.toString());
            event.put("toDate",    toDate.toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "leave.status.updated",
                    event);

            log.info("Published leave.status.updated event for: {}", email);

        } catch (Exception e) {
            log.error("Failed to publish leave status update: {}", e.getMessage());
        }
    }
}