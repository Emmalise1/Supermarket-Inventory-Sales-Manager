package com.supermarket.dto;

import com.supermarket.domain.Sale;
import com.supermarket.domain.SaleItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTOs for POS sales. */
public final class SaleDtos {

    private SaleDtos() {
    }

    public record SaleItemRequest(
            @NotNull Long productId,
            @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
    }

    /** Price always comes from the server, never from the client. */
    public record SaleRequest(
            Long branchId,
            @NotEmpty @Valid List<SaleItemRequest> items) {
    }

    public record SaleItemDto(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {

        public static SaleItemDto from(SaleItem item) {
            return new SaleItemDto(item.getProductId(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
        }
    }

    public record SaleDto(
            Long id,
            String saleNumber,
            Long branchId,
            Long cashierId,
            BigDecimal totalAmount,
            int itemCount,
            String status,
            Instant createdAt,
            List<SaleItemDto> items) {

        public static SaleDto from(Sale sale, List<SaleItem> items) {
            return new SaleDto(
                    sale.getId(),
                    sale.getSaleNumber(),
                    sale.getBranchId(),
                    sale.getCashierId(),
                    sale.getTotalAmount(),
                    sale.getItemCount(),
                    sale.getStatus(),
                    sale.getCreatedAt(),
                    items == null ? List.of() : items.stream().map(SaleItemDto::from).toList());
        }

        public static SaleDto summary(Sale sale) {
            return from(sale, null);
        }
    }
}
