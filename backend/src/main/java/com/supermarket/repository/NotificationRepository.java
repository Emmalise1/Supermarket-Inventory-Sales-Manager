package com.supermarket.repository;

import com.supermarket.document.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    List<Notification> findTop50ByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findTop50ByBranchIdAndUserIdIsNullOrderByCreatedAtDesc(Long branchId);

    long countByUserIdAndReadFalse(Long userId);
}
