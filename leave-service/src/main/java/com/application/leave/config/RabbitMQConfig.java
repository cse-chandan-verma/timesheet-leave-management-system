package com.application.leave.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME             = "tms.exchange";
    public static final String LEAVE_APPLIED_QUEUE       = "leave.applied.queue";
    public static final String LEAVE_APPLIED_ROUTING_KEY = "leave.applied";

    @Bean
    public TopicExchange tmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue leaveAppliedQueue() {
        return new Queue(LEAVE_APPLIED_QUEUE, true);
    }

    @Bean
    public Binding leaveBinding(Queue leaveAppliedQueue,
                                 TopicExchange tmsExchange) {
        return BindingBuilder.bind(leaveAppliedQueue)
                .to(tmsExchange)
                .with(LEAVE_APPLIED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
