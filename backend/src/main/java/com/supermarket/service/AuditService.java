package com.supermarket.service;

import com.supermarket.document.AuditLog;
import com.supermarket.repository.AuditLogRepository;
import com.supermarket.security.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Writes audit history entries to MongoDB.
 * Audit writes never break the business operation that triggered them.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuthContext authContext;

    public AuditService(AuditLogRepository auditLogRepository, AuthContext authContext) {
        this.auditLogRepository = auditLogRepository;
        this.authContext = authContext;
    }

    public void record(String action, String entityType, Object entityId, String details) {
        try {
            AuthContext.AuthUser user = authContext.current();
            AuditLog entry = new AuditLog(
                    user == null ? null : user.id(),
                    user == null ? null : user.email(),
                    user == null ? null : user.role().name(),
                    user == null ? null : user.branchId(),
                    action,
                    entityType,
                    entityId == null ? null : String.valueOf(entityId),
                    details);
            auditLogRepository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Audit write failed ({} {}): {}", action, entityType, ex.getMessage());
        }
    }

    public List<AuditLog> recent(Long branchId) {
        if (branchId == null) {
            return auditLogRepository.findTop50ByOrderByCreatedAtDesc();
        }
        return auditLogRepository.findTop50ByBranchIdOrderByCreatedAtDesc(branchId);
    }
}
