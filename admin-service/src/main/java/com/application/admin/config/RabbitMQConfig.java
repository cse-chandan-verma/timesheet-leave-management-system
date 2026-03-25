package com.application.admin.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Must match queue names used in Timesheet and Leave services
    public static final String TIMESHEET_QUEUE = "timesheet.events";
    public static final String LEAVE_QUEUE     = "leave.events";

    @Bean
    public Queue timesheetQueue() {
        return new Queue(TIMESHEET_QUEUE, true);
    }

    @Bean
    public Queue leaveQueue() {
        return new Queue(LEAVE_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}