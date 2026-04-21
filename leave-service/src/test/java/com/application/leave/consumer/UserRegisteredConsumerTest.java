package com.application.leave.consumer;

import com.application.leave.service.LeaveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegisteredConsumer — Unit Tests")
class UserRegisteredConsumerTest {

    @Mock
    private LeaveService leaveService;

    @InjectMocks
    private UserRegisteredConsumer consumer;

    @Test
    @DisplayName("handleUserRegistered() — valid userId")
    void handleUserRegistered_Valid_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 100L);

        consumer.handleUserRegistered(event);

        verify(leaveService).initializeLeaveBalance(100L);
    }

    @Test
    @DisplayName("handleUserRegistered() — missing userId")
    void handleUserRegistered_Missing_Skips() {
        Map<String, Object> event = new HashMap<>();

        consumer.handleUserRegistered(event);

        verifyNoInteractions(leaveService);
    }
}
