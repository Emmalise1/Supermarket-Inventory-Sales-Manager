package com.supermarket.repository;

import com.supermarket.document.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findTop50ByOrderByCreatedAtDesc();

    List<AuditLog> findTop50ByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<AuditLog> findTop50ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId, Pageable pageable);
}
