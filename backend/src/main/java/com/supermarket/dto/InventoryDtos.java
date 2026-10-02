package com.supermarket.dto;

import com.supermarket.domain.StockMovement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** DTOs for inventory operations (goods receiving, stock adjustments). */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    /** Receiving goods from a supplier: adds stock (+). */
    public record GoodsReceiptRequest(
            @NotNull Long productId,
            @Min(value = 1, message = "Quantity must be at least 1") int quantity,
            String reference,
            String note) {
    }

    /**
     * Manual stock correction (+/-), e.g. damaged or expired goods.
     * A negative adjustment may never make stock negative.
     */
    public record StockAdjustmentRequest(
            @NotNull Long productId,
            int quantityChange,
            @NotBlank String reason) {
    }

    /** currentStock is the product's live stock in MySQL at read time. */
    public record StockMovementDto(
            Long id,
            String movementType,
            Long productId,
            String productName,
            Long branchId,
            int quantityChange,
            int currentStock,
            String reason,
            Long userId,
            Instant createdAt) {

        public static StockMovementDto from(StockMovement m, String productName, int currentStock) {
            return new StockMovementDto(
                    m.getId(),
                    m.getMovementType().name(),
                    m.getProductId(),
                    productName,
                    m.getBranchId(),
                    m.getQuantityChange(),
                    currentStock,
                    m.getReason(),
                    m.getUserId(),
                    m.getCreatedAt());
        }
    }
}
