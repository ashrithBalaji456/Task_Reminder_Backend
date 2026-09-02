package com.application.taskmanager.notification.dto;

import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogResponse {

    private Long id;
    private Long taskOccurrenceId;
    private NotificationType notificationType;
    private String periodIdentifier;
    private Instant scheduledFor;
    private NotificationStatus status;
    private int attemptCount;
    private Instant sentAt;
    private Instant createdAt;
}
