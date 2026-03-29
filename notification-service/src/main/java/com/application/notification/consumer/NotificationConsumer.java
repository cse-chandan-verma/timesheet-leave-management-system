package com.application.notification.consumer;

import com.application.notification.config.RabbitMQConfig;
import com.application.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.REGISTRATION_QUEUE)
    public void consumeRegistrationEvent(Map<String, Object> event) {
        log.info("Received registration event: {}", event);
        String email = (String) event.get("email");
        String name = (String) event.get("fullName");

        if (name == null || name.isBlank()) {
            name = "User";
        }

        emailService.sendRegistrationEmail(email, name);
    }

    @RabbitListener(queues = RabbitMQConfig.PROFILE_UPDATE_QUEUE)
    public void consumeProfileUpdateEvent(Map<String, Object> event) {
        log.info("Received profile update event: {}", event);
        String email = (String) event.get("email");
        String name = (String) event.get("fullName");

        if (name == null || name.isBlank()) {
            name = "User";
        }

        emailService.sendProfileUpdateEmail(email, name);
    }

    @RabbitListener(queues = RabbitMQConfig.PASSWORD_CHANGE_QUEUE)
    public void consumePasswordChangeEvent(Map<String, Object> event) {
        log.info("Received password change event: {}", event);
        String email = (String) event.get("email");

        emailService.sendPasswordChangeEmail(email);
    }

    @RabbitListener(queues = {
            RabbitMQConfig.LEAVE_UPDATE_QUEUE,
            RabbitMQConfig.LEAVE_APPROVE_QUEUE,
            RabbitMQConfig.LEAVE_REJECT_QUEUE
    })
    public void consumeLeaveUpdateEvent(Map<String, Object> event) {
        log.info("Received leave update event: {}", event);
        String email = (String) event.get("email");
        String name = (String) event.get("fullName");
        String status = (String) event.get("status");
        String remarks = (String) event.get("remarks");
        String leaveType = (String) event.get("leaveType");
        String fromDate = (String) event.get("fromDate");
        String toDate = (String) event.get("toDate");

        if (name == null || name.isBlank())
            name = "User";

        emailService.sendLeaveStatusEmail(email, name, status, remarks, leaveType, fromDate, toDate);
    }

    @RabbitListener(queues = RabbitMQConfig.TIMESHEET_UPDATE_QUEUE)
    public void consumeTimesheetUpdateEvent(Map<String, Object> event) {
        log.info("Received timesheet update event: {}", event);
        String email = (String) event.get("email");
        String name = (String) event.get("fullName");
        String status = (String) event.get("status");
        String remarks = (String) event.get("remarks");
        String weekStart = (String) event.get("weekStart");

        if (name == null || name.isBlank())
            name = "User";

        emailService.sendTimesheetStatusEmail(email, name, status, remarks, weekStart);
    }

    @RabbitListener(queues = RabbitMQConfig.ROLE_UPDATE_QUEUE)
    public void consumeRoleUpdateEvent(Map<String, Object> event) {
        log.info("Received role update event: {}", event);
        String email = (String) event.get("email");
        String name = (String) event.get("fullName");
        String oldRole = (String) event.get("oldRole");
        String newRole = (String) event.get("newRole");

        if (name == null || name.isBlank())
            name = "User";

        emailService.sendRoleUpdateEmail(email, name, oldRole, newRole);
    }
}
