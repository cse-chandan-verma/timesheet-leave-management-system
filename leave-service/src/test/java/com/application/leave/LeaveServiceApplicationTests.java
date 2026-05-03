package com.application.leave;

import com.application.leave.feign.AuthServiceClient;
import com.application.leave.messaging.LeaveEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LeaveServiceApplicationTests {

	// Mock outbound clients so context loads without live RabbitMQ / Eureka
	@MockitoBean
	private AuthServiceClient authServiceClient;

	@MockitoBean
	private LeaveEventPublisher leaveEventPublisher;

	// Mock RabbitMQ infrastructure so RabbitMQConfig can wire without a broker
	@MockitoBean
	private ConnectionFactory connectionFactory;

	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}


