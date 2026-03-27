package com.application.admin.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.application.admin.config.RabbitMQConfig;
import com.application.admin.entity.AdminNotification;
import com.application.admin.repository.AdminNotificationRepository;

import java.util.Map;

/**
 * Consumes events from the message broker and converts them to 
 * persistence-ready AdminNotifications.
 */
@Component
public class NotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationConsumer.class);

    private final AdminNotificationRepository notificationRepository;

    public NotificationConsumer(AdminNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Handles newly submitted timesheets.
     */
    @RabbitListener(queues = RabbitMQConfig.TIMESHEET_QUEUE)
    public void handleTimesheetEvent(Map<String, Object> event) {
        log.info("Received TIMESHEET event from queue: {}", event);
        
        AdminNotification notification = new AdminNotification();
        notification.setEventType("TIMESHEET");
        notification.setUserEmail(String.valueOf(event.getOrDefault("employeeEmail", "unknown@system")));
        notification.setMessage(String.valueOf(event.getOrDefault("message", "A new timesheet was submitted.")));
        notification.setStatus(String.valueOf(event.getOrDefault("status", "SUBMITTED")));
        
        notificationRepository.save(notification);
        log.debug("Saved AdminNotification for timesheet submission.");
    }

    /**
     * Handles newly applied leave requests.
     */
    @RabbitListener(queues = RabbitMQConfig.LEAVE_QUEUE)
    public void handleLeaveEvent(Map<String, Object> event) {
        log.info("Received LEAVE event from queue: {}", event);
        
        AdminNotification notification = new AdminNotification();
        notification.setEventType("LEAVE");
        notification.setUserEmail(String.valueOf(event.getOrDefault("employeeEmail", "unknown@system")));
        notification.setMessage(String.valueOf(event.getOrDefault("message", "A new leave request was applied.")));
        notification.setStatus(String.valueOf(event.getOrDefault("status", "SUBMITTED")));
        
        notificationRepository.save(notification);
        log.debug("Saved AdminNotification for leave application.");
    }
}