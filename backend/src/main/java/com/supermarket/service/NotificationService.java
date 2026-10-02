package com.supermarket.service;

import com.supermarket.document.Notification;
import com.supermarket.domain.User;
import com.supermarket.mq.DomainEvent;
import com.supermarket.repository.NotificationRepository;
import com.supermarket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Notifications persisted in MongoDB (document-oriented data). */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Called by the RabbitMQ consumer (or the direct fallback when the
     * broker is down). Targets a specific user, or every active user of
     * the event's branch when {@code targetUserId} is null.
     */
    public void createFromEvent(DomainEvent event) {
        if (event.targetUserId() != null) {
            notificationRepository.save(new Notification(
                    event.targetUserId(), event.branchId(), event.type(), event.title(), event.message()));
            return;
        }
        List<Long> recipients = new ArrayList<>();
        if (event.branchId() != null) {
            userRepository.findByBranchId(event.branchId()).stream()
                    .filter(User::isActive)
                    .forEach(u -> recipients.add(u.getId()));
        }
        if (recipients.isEmpty()) {
            // No branch users (or broadcast): store as branch-wide notification.
            notificationRepository.save(new Notification(
                    null, event.branchId(), event.type(), event.title(), event.message()));
            return;
        }
        recipients.forEach(userId -> notificationRepository.save(new Notification(
                userId, event.branchId(), event.type(), event.title(), event.message())));
        log.debug("Stored {} notifications for event {}", recipients.size(), event.type());
    }

    public List<Notification> recentForUser(Long userId, Long branchId) {
        List<Notification> own = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        List<Notification> branchWide = branchId == null
                ? List.of()
                : notificationRepository.findTop50ByBranchIdAndUserIdIsNullOrderByCreatedAtDesc(branchId);
        List<Notification> merged = new ArrayList<>(own);
        merged.addAll(branchWide);
        merged.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return merged.size() > 50 ? new ArrayList<>(merged.subList(0, 50)) : merged;
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public Notification markRead(String id, Long userId) {
        Optional<Notification> found = notificationRepository.findById(id);
        if (found.isEmpty()) {
            return null;
        }
        Notification notification = found.get();
        if (notification.getUserId() != null && !notification.getUserId().equals(userId)) {
            return null;
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
