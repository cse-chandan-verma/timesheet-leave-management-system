package com.application.leave.consumer;

import com.application.leave.config.RabbitMQConfig;
import com.application.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final LeaveService leaveService;

    /**
     * Listen for USER_REGISTERED events.
     * message is a Map containing "userId"
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    public void handleUserRegistered(Map<String, Object> event) {
        log.info("Received USER_REGISTERED event: {}", event);

        try {
            Object userIdObj = event.get("userId");
            if (userIdObj == null) {
                log.error("Event missing userId: {}", event);
                return;
            }

            // RabbitMQ might send Integer or Long
            Long userId = Long.valueOf(userIdObj.toString());
            
            leaveService.initializeLeaveBalance(userId);
            log.info("Successfully initialized leave balances for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage());
        }
    }
}
