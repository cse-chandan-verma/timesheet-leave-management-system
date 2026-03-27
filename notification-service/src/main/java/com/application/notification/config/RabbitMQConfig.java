package com.application.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "tms.exchange";
    
    public static final String REGISTRATION_QUEUE = "notification.registration.queue";
    public static final String PROFILE_UPDATE_QUEUE = "notification.profile.queue";
    public static final String PASSWORD_CHANGE_QUEUE = "notification.password.queue";
    public static final String LEAVE_UPDATE_QUEUE = "notification.leave.queue";
    public static final String TIMESHEET_UPDATE_QUEUE = "notification.timesheet.queue";
    public static final String ROLE_UPDATE_QUEUE = "notification.role.queue";

    @Bean
    public TopicExchange tmsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue registrationQueue() {
        return new Queue(REGISTRATION_QUEUE, true);
    }

    @Bean
    public Queue profileUpdateQueue() {
        return new Queue(PROFILE_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue passwordChangeQueue() {
        return new Queue(PASSWORD_CHANGE_QUEUE, true);
    }

    @Bean
    public Queue leaveUpdateQueue() {
        return new Queue(LEAVE_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue timesheetUpdateQueue() {
        return new Queue(TIMESHEET_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue roleUpdateQueue() {
        return new Queue(ROLE_UPDATE_QUEUE, true);
    }

    @Bean
    public Binding registrationBinding(Queue registrationQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(registrationQueue).to(tmsExchange).with("user.registered");
    }

    @Bean
    public Binding profileUpdateBinding(Queue profileUpdateQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(profileUpdateQueue).to(tmsExchange).with("user.profile.updated");
    }

    @Bean
    public Binding passwordChangeBinding(Queue passwordChangeQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(passwordChangeQueue).to(tmsExchange).with("user.password.changed");
    }

    @Bean
    public Binding leaveUpdateBinding(Queue leaveUpdateQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(leaveUpdateQueue).to(tmsExchange).with("leave.status.updated");
    }

    @Bean
    public Binding timesheetUpdateBinding(Queue timesheetUpdateQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(timesheetUpdateQueue).to(tmsExchange).with("timesheet.status.updated");
    }

    @Bean
    public Binding roleUpdateBinding(Queue roleUpdateQueue, TopicExchange tmsExchange) {
        return BindingBuilder.bind(roleUpdateQueue).to(tmsExchange).with("user.role.updated");
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
