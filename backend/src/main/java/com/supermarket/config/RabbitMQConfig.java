package com.supermarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology: one topic exchange, JSON-serialized events.
 *
 * <p>Flow: Spring Boot publishes domain events -&gt; RabbitMQ -&gt;
 * consumers -&gt; notifications / audit side effects.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "supermarket.events";
    public static final String NOTIFICATION_QUEUE = "supermarket.notifications";

    public static final String RK_SALE_COMPLETED = "sale.completed";
    public static final String RK_LOW_STOCK = "stock.low";
    public static final String RK_GOODS_RECEIVED = "stock.received";
    public static final String RK_STOCK_ADJUSTED = "stock.adjusted";

    @Bean
    public TopicExchange supermarketExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange supermarketExchange) {
        return BindingBuilder.bind(notificationQueue).to(supermarketExchange).with("#");
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
