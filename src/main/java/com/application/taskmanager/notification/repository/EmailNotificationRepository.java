package com.application.taskmanager.notification.repository;

import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    Page<EmailNotification> findByUserId(Long userId, Pageable pageable);

    Page<EmailNotification> findByUserIdAndNotificationType(Long userId, NotificationType notificationType, Pageable pageable);

    Optional<EmailNotification> findByIdAndUserId(Long id, Long userId);

    boolean existsByTaskOccurrenceIdAndNotificationType(Long taskOccurrenceId, NotificationType notificationType);

    boolean existsByTaskOccurrenceIdAndNotificationTypeAndScheduledFor(Long taskOccurrenceId, NotificationType notificationType, Instant scheduledFor);

    boolean existsByUserIdAndNotificationTypeAndPeriodIdentifier(Long userId, NotificationType notificationType, String periodIdentifier);

    @Query("SELECT n FROM EmailNotification n WHERE n.status = :status AND n.scheduledFor <= :now AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now) AND n.attemptCount < n.maxAttempts")
    List<EmailNotification> findPendingNotificationsDue(
            @Param("status") NotificationStatus status,
            @Param("now") Instant now
    );

    @Modifying
    @Query("UPDATE EmailNotification n SET n.status = 'CANCELLED' WHERE n.taskOccurrence.id = :taskOccurrenceId AND n.status = 'PENDING'")
    int cancelPendingNotificationsForOccurrence(@Param("taskOccurrenceId") Long taskOccurrenceId);
}
