package com.application.admin.consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.application.admin.config.RabbitMQConfig;
import com.application.admin.dto.NotificationEventDTO;
import com.application.admin.entity.AdminNotification;
import com.application.admin.repository.AdminNotificationRepository;

@Component
public class NotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationConsumer.class);

    private final AdminNotificationRepository notificationRepository;

    public NotificationConsumer(AdminNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.TIMESHEET_QUEUE)
    public void handleTimesheetEvent(NotificationEventDTO event) {
        log.info("Received TIMESHEET event: {}", event);
        saveNotification(event, "TIMESHEET");
    }

    @RabbitListener(queues = RabbitMQConfig.LEAVE_QUEUE)
    public void handleLeaveEvent(NotificationEventDTO event) {
        log.info("Received LEAVE event: {}", event);
        saveNotification(event, "LEAVE");
    }

    private void saveNotification(NotificationEventDTO event, String type) {
        AdminNotification notification = new AdminNotification();
        notification.setEventType(type);
        notification.setUserEmail(event.getUserEmail());
        notification.setMessage(event.getMessage());
        notification.setStatus(event.getStatus());
        notificationRepository.save(notification);
    }
}