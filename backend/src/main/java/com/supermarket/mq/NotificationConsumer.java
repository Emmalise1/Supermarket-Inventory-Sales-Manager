package com.supermarket.mq;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous consumer: RabbitMQ event -&gt; notification stored in MongoDB.
 * This is the "Spring Boot -> RabbitMQ -> Consumers -> Notifications" part
 * of the architecture.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onDomainEvent(DomainEvent event) {
        log.info("Consuming event '{}' for branch {}", event.type(), event.branchId());
        notificationService.createFromEvent(event);
    }
}
