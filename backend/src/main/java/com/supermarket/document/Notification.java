package com.supermarket.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * User-facing notification stored in MongoDB. Notifications are produced
 * asynchronously from domain events published to RabbitMQ.
 */
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private Long branchId;

    /** e.g. LOW_STOCK, SALE_COMPLETED, GOODS_RECEIVED. */
    private String type;

    private String title;

    private String message;

    private boolean read;

    @Indexed
    private Instant createdAt = Instant.now();

    public Notification() {
    }

    public Notification(Long userId, Long branchId, String type, String title, String message) {
        this.userId = userId;
        this.branchId = branchId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
