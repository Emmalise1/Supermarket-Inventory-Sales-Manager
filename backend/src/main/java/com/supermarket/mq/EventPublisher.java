package com.supermarket.mq;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to RabbitMQ.
 *
 * <p>If the broker is unreachable the publish is retried never - the event is
 * applied <b>directly</b> as a fallback so the application keeps working
 * without RabbitMQ, and a warning is logged.</p>
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;

    public EventPublisher(RabbitTemplate rabbitTemplate, NotificationService notificationService) {
        this.rabbitTemplate = rabbitTemplate;
        this.notificationService = notificationService;
    }

    /**
     * @return true when the event went through RabbitMQ, false when the
     *         synchronous fallback was used instead.
     */
    public boolean publish(String routingKey, DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
            return true;
        } catch (RuntimeException ex) {
            log.warn("RabbitMQ unavailable, applying event '{}' directly: {}", routingKey, ex.getMessage());
            try {
                notificationService.createFromEvent(event);
            } catch (RuntimeException fallbackEx) {
                log.warn("Fallback notification failed: {}", fallbackEx.getMessage());
            }
            return false;
        }
    }
}
