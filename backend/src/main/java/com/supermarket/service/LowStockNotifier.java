package com.supermarket.service;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.domain.Product;
import com.supermarket.mq.DomainEvent;
import com.supermarket.mq.EventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes a LOW_STOCK notification event whenever a product's stock
 * falls to or below its threshold. Delivered through RabbitMQ.
 */
@Component
public class LowStockNotifier {

    private final EventPublisher eventPublisher;

    public LowStockNotifier(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** @return true when an event was published (stock is at/below threshold). */
    public boolean check(Product product) {
        if (product.getQuantityInStock() > product.getLowStockThreshold()) {
            return false;
        }
        eventPublisher.publish(RabbitMQConfig.RK_LOW_STOCK, DomainEvent.of(
                "LOW_STOCK",
                product.getBranchId(),
                null,
                "Low stock: " + product.getName(),
                "Product '" + product.getName() + "' (" + product.getBarcode() + ") has only "
                        + product.getQuantityInStock() + " unit(s) left (threshold "
                        + product.getLowStockThreshold() + ")."));
        return true;
    }
}
