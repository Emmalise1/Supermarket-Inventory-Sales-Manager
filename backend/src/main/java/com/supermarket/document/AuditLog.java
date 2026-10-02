package com.supermarket.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Audit trail entry stored in MongoDB (document-oriented data). */
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private Long actorId;

    private String actorEmail;

    private String role;

    @Indexed
    private Long branchId;

    /** e.g. PRODUCT_UPDATED, SALE_COMPLETED, USER_LOGIN, STOCK_ADJUSTED. */
    private String action;

    private String entityType;

    private String entityId;

    private String details;

    @Indexed
    private Instant createdAt = Instant.now();

    public AuditLog() {
    }

    public AuditLog(Long actorId, String actorEmail, String role, Long branchId,
                    String action, String entityType, String entityId, String details) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.role = role;
        this.branchId = branchId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
