package com.application.admin.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME    = "tms.exchange";

    // Queue names — these must match the @RabbitListener queue names in NotificationConsumer
    public static final String TIMESHEET_QUEUE  = "timesheet.events";
    public static final String LEAVE_QUEUE      = "leave.events";

    // Routing keys — these must match what the publisher services actually send
    // TimesheetEventPublisher.publishTimesheetSubmitted() → "timesheet.submitted"
    // LeaveEventPublisher.publishLeaveApplied()           → "leave.applied"
    public static final String TIMESHEET_RK     = "timesheet.submitted";
    public static final String LEAVE_RK         = "leave.applied";

    @Bean
    public TopicExchange tmsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue timesheetQueue() {
        return new Queue(TIMESHEET_QUEUE, true);
    }

    @Bean
    public Queue leaveQueue() {
        return new Queue(LEAVE_QUEUE, true);
    }

    /**
     * Binds timesheet.events queue to tms.exchange via routing key "timesheet.submitted".
     * This routes the TIMESHEET_SUBMITTED events from timesheet-service to admin's audit log.
     */
    @Bean
    public Binding timesheetBinding(Queue timesheetQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(timesheetQueue)
                .to(tmsExchange)
                .with(TIMESHEET_RK);
    }

    /**
     * Binds leave.events queue to tms.exchange via routing key "leave.applied".
     * This routes the LEAVE_APPLIED events from leave-service to admin's audit log.
     */
    @Bean
    public Binding leaveBinding(Queue leaveQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(leaveQueue)
                .to(tmsExchange)
                .with(LEAVE_RK);
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