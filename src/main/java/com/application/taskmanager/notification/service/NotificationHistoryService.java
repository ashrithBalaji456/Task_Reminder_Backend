package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.dto.NotificationLogResponse;
import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationType;
import com.application.taskmanager.notification.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final EmailNotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationLogResponse> getNotificationHistory(Long userId, NotificationType type, Pageable pageable) {
        Page<EmailNotification> page;
        if (type != null) {
            page = notificationRepository.findByUserIdAndNotificationType(userId, type, pageable);
        } else {
            page = notificationRepository.findByUserId(userId, pageable);
        }

        return page.map(this::toResponse);
    }

    private NotificationLogResponse toResponse(EmailNotification n) {
        return NotificationLogResponse.builder()
                .id(n.getId())
                .taskOccurrenceId(n.getTaskOccurrence() != null ? n.getTaskOccurrence().getId() : null)
                .notificationType(n.getNotificationType())
                .periodIdentifier(n.getPeriodIdentifier())
                .scheduledFor(n.getScheduledFor())
                .status(n.getStatus())
                .attemptCount(n.getAttemptCount())
                .sentAt(n.getSentAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
