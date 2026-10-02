package com.supermarket.mq;

import java.time.Instant;

/**
 * Domain event published to RabbitMQ and consumed asynchronously to
 * produce notifications. Serialized as JSON by the message converter.
 */
public record DomainEvent(
        String type,
        Long branchId,
        Long targetUserId,
        String title,
        String message,
        Instant occurredAt) {

    public static DomainEvent of(String type, Long branchId, Long targetUserId, String title, String message) {
        return new DomainEvent(type, branchId, targetUserId, title, message, Instant.now());
    }
}
