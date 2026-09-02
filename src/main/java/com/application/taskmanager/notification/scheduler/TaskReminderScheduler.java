package com.application.taskmanager.notification.scheduler;

import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.entity.NotificationType;
import com.application.taskmanager.notification.entity.PushSubscription;
import com.application.taskmanager.notification.repository.EmailNotificationRepository;
import com.application.taskmanager.notification.repository.PushSubscriptionRepository;
import com.application.taskmanager.notification.service.EmailSenderService;
import com.application.taskmanager.notification.service.WebPushService;
import com.application.taskmanager.notification.template.TaskReminderEmailTemplate;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.entity.UserEmailPreference;
import com.application.taskmanager.user.repository.UserEmailPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskReminderScheduler {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserEmailPreferenceRepository preferenceRepository;
    private final EmailSenderService emailSenderService;
    private final WebPushService webPushService;
    private final TaskReminderEmailTemplate reminderEmailTemplate;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(fixedRate = 60000)
    public void scheduledCronJob() {
        if (!schedulingEnabled) {
            log.warn("[REMINDER-SCHEDULER] Scheduling is explicitly disabled via configuration");
            return;
        }
        try {
            processTaskReminders();
        } catch (Exception ex) {
            log.error("[REMINDER-SCHEDULER] Exception during task reminder processing loop: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void processTaskReminders() {
        Instant now = Instant.now();
        ZoneId serverZone = ZoneId.systemDefault();

        log.info("[REMINDER-SCHEDULER] START | Server time: {} | Server Zone: {} | UTC Instant: {}",
                LocalDateTime.now(serverZone), serverZone, now);

        // 1. Transactionally discover and enqueue due reminders
        enqueueDueReminders(now);
        emailNotificationRepository.flush();

        // 2. Fetch notifications ready for dispatch
        List<Long> pendingIds = getPendingNotificationIds(now);
        log.info("[REMINDER-SCHEDULER] Found {} pending reminder notifications due for dispatch", pendingIds.size());

        // 3. Process each notification independently
        for (Long notificationId : pendingIds) {
            try {
                processSingleNotification(notificationId);
            } catch (Exception e) {
                log.error("[REMINDER-SCHEDULER] Error processing notification ID {}: {}", notificationId, e.getMessage(), e);
            }
        }

        log.info("[REMINDER-SCHEDULER] END");
    }

    @Transactional
    public void enqueueDueReminders(Instant now) {
        List<TaskOccurrence> occurrencesToRemind = taskOccurrenceRepository.findAll().stream()
                .filter(o -> o.getStatus() == TaskStatus.PENDING)
                .filter(o -> o.getReminderScheduledAt() != null && !o.getReminderScheduledAt().isAfter(now))
                .toList();

        for (TaskOccurrence occurrence : occurrencesToRemind) {
            User user = occurrence.getUser();
            UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);
            if (preference != null && !preference.isTaskReminderEnabled() && !preference.isPushNotificationEnabled()) {
                log.info("[REMINDER-SCHEDULER] SKIPPED occurrence ID {} because all notification channels are disabled for user ID {}", occurrence.getId(), user.getId());
                continue;
            }

            boolean exists = emailNotificationRepository.existsByTaskOccurrenceIdAndNotificationType(
                    occurrence.getId(), NotificationType.TASK_REMINDER
            );

            if (!exists) {
                EmailNotification notification = EmailNotification.builder()
                        .user(user)
                        .taskOccurrence(occurrence)
                        .notificationType(NotificationType.TASK_REMINDER)
                        .scheduledFor(occurrence.getReminderScheduledAt())
                        .status(NotificationStatus.PENDING)
                        .attemptCount(0)
                        .maxAttempts(3)
                        .build();

                emailNotificationRepository.save(notification);
                log.info("[REMINDER-SCHEDULER] ENQUEUED PENDING reminder notification ID {} for task occurrence ID '{}' (due at {})",
                        notification.getId(), occurrence.getTitle(), occurrence.getDueDateTime());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getPendingNotificationIds(Instant now) {
        return emailNotificationRepository.findPendingNotificationsDue(NotificationStatus.PENDING, now)
                .stream()
                .map(EmailNotification::getId)
                .toList();
    }

    public void processSingleNotification(Long notificationId) {
        log.info("[REMINDER-DISPATCHER] Attempting to claim notification ID: {}", notificationId);
        ClaimedNotification claimed = claimNotification(notificationId);
        if (claimed == null) {
            log.info("[REMINDER-DISPATCHER] Notification ID {} skipped (already claimed or task not pending)", notificationId);
            return;
        }

        log.info("[REMINDER-DISPATCHER] Claimed notification ID: {} | User: {} | Task: '{}' | Attempt #{}",
                notificationId, claimed.userEmail, claimed.taskTitle, 1);

        boolean emailSuccess = false;
        boolean pushSuccess = false;
        String providerMsgId = null;
        String failureMessage = null;

        try {
            // Channel 1: Email via Brevo
            if (claimed.isEmailEnabled) {
                log.info("[REMINDER-DISPATCHER] [EMAIL] Sending Brevo email for notification ID {} to {}", notificationId, claimed.userEmail);
                try {
                    providerMsgId = emailSenderService.sendEmail(
                            claimed.userEmail, claimed.userName,
                            "⏰ Task Reminder: " + claimed.taskTitle,
                            claimed.emailHtml
                    );
                    emailSuccess = true;
                    log.info("[REMINDER-DISPATCHER] [EMAIL] Brevo email sent successfully. MsgId: {}", providerMsgId);
                } catch (Exception ex) {
                    log.error("[REMINDER-DISPATCHER] [EMAIL] Brevo delivery failed for notification ID {}: {}", notificationId, ex.getMessage(), ex);
                    failureMessage = "Email error: " + ex.getMessage();
                }
            } else {
                log.info("[REMINDER-DISPATCHER] [EMAIL] Skipped (channel disabled in user preferences)");
            }

            // Channel 2: Web Push Notifications
            if (claimed.isPushEnabled && claimed.activeSubscriptions != null && !claimed.activeSubscriptions.isEmpty()) {
                log.info("[REMINDER-DISPATCHER] [PUSH] Found {} active Web Push subscriptions for user {}", claimed.activeSubscriptions.size(), claimed.userName);
                for (PushSubscription sub : claimed.activeSubscriptions) {
                    try {
                        log.info("[REMINDER-DISPATCHER] [PUSH] Dispatching push to endpoint: {}", sub.getEndpoint());
                        boolean ok = webPushService.sendPushNotification(sub, claimed.pushTitle, claimed.pushBody, "/tasks");
                        if (ok) {
                            pushSuccess = true;
                            log.info("[REMINDER-DISPATCHER] [PUSH] Push notification delivered successfully");
                        }
                    } catch (Exception ex) {
                        log.error("[REMINDER-DISPATCHER] [PUSH] Push delivery failed for endpoint {}: {}", sub.getEndpoint(), ex.getMessage(), ex);
                    }
                }
            } else {
                log.info("[REMINDER-DISPATCHER] [PUSH] Skipped (channel disabled or no active device subscriptions)");
            }
        } catch (Exception ex) {
            failureMessage = ex.getMessage();
        }

        boolean overallSuccess = emailSuccess || pushSuccess || (!claimed.isEmailEnabled && !claimed.isPushEnabled);
        recordNotificationResult(notificationId, overallSuccess, providerMsgId != null ? providerMsgId : "MULTI_CHANNEL_SENT", failureMessage);
        log.info("[REMINDER-DISPATCHER] Notification ID {} completed with result: {}", notificationId, overallSuccess ? "SENT" : "FAILED");
    }

    @Transactional
    public ClaimedNotification claimNotification(Long notificationId) {
        Optional<EmailNotification> opt = emailNotificationRepository.findById(notificationId);
        if (opt.isEmpty()) return null;

        EmailNotification notification = opt.get();
        TaskOccurrence occurrence = notification.getTaskOccurrence();

        if (occurrence != null && occurrence.getStatus() != TaskStatus.PENDING) {
            notification.setStatus(NotificationStatus.CANCELLED);
            notification.setFailureReason("Task occurrence is " + occurrence.getStatus());
            emailNotificationRepository.save(notification);
            return null;
        }

        if (notification.getStatus() != NotificationStatus.PENDING) {
            return null;
        }

        notification.setStatus(NotificationStatus.PROCESSING);
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        emailNotificationRepository.saveAndFlush(notification);

        User user = notification.getUser();
        UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);

        long remainingMins = Math.max(0, Duration.between(Instant.now(), occurrence.getDueDateTime()).toMinutes());
        String remainingStr = remainingMins > 60
                ? (remainingMins / 60) + " hours " + (remainingMins % 60) + " minutes"
                : remainingMins + " minutes";

        String dueTimeStr = occurrence.getDueTime().toString() + " (" + user.getTimezone() + ")";

        String html = reminderEmailTemplate.buildTaskReminderHtml(
                user.getName(), occurrence.getTitle(), occurrence.getDescription(),
                occurrence.getPriority().name(), dueTimeStr, remainingStr
        );

        String pushTitle = (occurrence.getPriority() == com.application.taskmanager.task.entity.Priority.HIGH)
                ? "🔴 High Priority Task Reminder"
                : "Task Reminder 🌸";
        String pushBody = String.format("%s is due in %s", occurrence.getTitle(), remainingStr);

        List<PushSubscription> activeSubs = pushSubscriptionRepository.findByUserIdAndActiveTrue(user.getId());

        return new ClaimedNotification(
                notificationId,
                user.getEmail(),
                user.getName(),
                occurrence.getTitle(),
                html,
                pushTitle,
                pushBody,
                preference == null || preference.isTaskReminderEnabled(),
                preference == null || preference.isPushNotificationEnabled(),
                activeSubs
        );
    }

    @Transactional
    public void recordNotificationResult(Long notificationId, boolean success, String msgId, String failureReason) {
        Optional<EmailNotification> opt = emailNotificationRepository.findById(notificationId);
        if (opt.isEmpty()) return;

        EmailNotification notification = opt.get();
        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setProviderMessageId(msgId);
        } else {
            notification.setFailureReason(failureReason);
            if (notification.getAttemptCount() >= notification.getMaxAttempts()) {
                notification.setStatus(NotificationStatus.FAILED);
            } else {
                notification.setStatus(NotificationStatus.PENDING);
                long delayMinutes = notification.getAttemptCount() == 1 ? 5 : (notification.getAttemptCount() == 2 ? 15 : 60);
                notification.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(delayMinutes)));
            }
        }
        emailNotificationRepository.save(notification);
    }

    public record ClaimedNotification(
            Long notificationId,
            String userEmail,
            String userName,
            String taskTitle,
            String emailHtml,
            String pushTitle,
            String pushBody,
            boolean isEmailEnabled,
            boolean isPushEnabled,
            List<PushSubscription> activeSubscriptions
    ) {}
}
