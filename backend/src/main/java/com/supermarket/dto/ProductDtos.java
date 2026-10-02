package com.supermarket.dto;

import com.supermarket.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** DTOs for products, barcode lookup and the Redis performance comparison. */
public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductDto(
            Long id,
            String barcode,
            String name,
            Long categoryId,
            Long supplierId,
            Long branchId,
            BigDecimal costPrice,
            BigDecimal price,
            int quantityInStock,
            int lowStockThreshold,
            boolean active,
            Instant updatedAt) {

        public static ProductDto from(Product p) {
            return new ProductDto(
                    p.getId(), p.getBarcode(), p.getName(), p.getCategoryId(), p.getSupplierId(),
                    p.getBranchId(), p.getCostPrice(), p.getPrice(), p.getQuantityInStock(),
                    p.getLowStockThreshold(), p.isActive(), p.getUpdatedAt());
        }
    }

    public record ProductRequest(
            @NotBlank String barcode,
            @NotBlank String name,
            Long categoryId,
            Long supplierId,
            @NotNull Long branchId,
            BigDecimal costPrice,
            @NotNull @DecimalMin(value = "0.00") BigDecimal price,
            @Min(value = 0) Integer quantityInStock,
            @Min(value = 0) Integer lowStockThreshold,
            Boolean active) {
    }

    /** Result of GET /api/products/barcode/{barcode} - shows whether Redis answered. */
    public record BarcodeLookupResponse(
            ProductDto product,
            boolean cacheHit,
            long elapsedMs) {
    }

    /**
     * Actual measured timings from a live comparison run on this machine.
     * Values are measured, never invented.
     */
    public record BarcodeBenchmarkResponse(
            String barcode,
            int iterations,
            double avgDbLookupMs,
            double avgRedisLookupMs,
            double speedup,
            String note) {
    }
}
