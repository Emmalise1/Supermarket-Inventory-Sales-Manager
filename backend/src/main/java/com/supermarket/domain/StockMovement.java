package com.supermarket.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Immutable record of every stock change: goods receiving (+), sales (-)
 * and manual adjustments (+/-). Gives a full audit trail for inventory.
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_movements_product", columnList = "productId"),
        @Index(name = "idx_movements_branch", columnList = "branchId")
})
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** Signed quantity: positive adds stock, negative removes stock. */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(length = 255)
    private String reason;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public StockMovement() {
    }

    public StockMovement(MovementType type, Long productId, Long branchId,
                         int quantityChange, String reason, Long userId) {
        this.movementType = type;
        this.productId = productId;
        this.branchId = branchId;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
