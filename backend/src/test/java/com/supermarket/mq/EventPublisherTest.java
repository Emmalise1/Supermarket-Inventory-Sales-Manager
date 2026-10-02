package com.supermarket.mq;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RabbitMQ event tests: publishes to the exchange, and falls back to a
 * direct notification when the broker is unreachable.
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private NotificationService notificationService;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(rabbitTemplate, notificationService);
    }

    private DomainEvent event() {
        return DomainEvent.of("LOW_STOCK", 1L, null, "Low stock", "Only 2 left");
    }

    @Test
    void publish_sendsJsonEventToExchange() {
        boolean viaRabbit = eventPublisher.publish(RabbitMQConfig.RK_LOW_STOCK, event());

        assertThat(viaRabbit).isTrue();
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.RK_LOW_STOCK), any(DomainEvent.class));
        verify(notificationService, never()).createFromEvent(any());
    }

    @Test
    void publish_whenBrokerDown_fallsBackToDirectNotification() {
        doThrow(new RuntimeException("connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        boolean viaRabbit = eventPublisher.publish(RabbitMQConfig.RK_LOW_STOCK, event());

        assertThat(viaRabbit).isFalse();
        verify(notificationService).createFromEvent(any(DomainEvent.class));
    }
}
