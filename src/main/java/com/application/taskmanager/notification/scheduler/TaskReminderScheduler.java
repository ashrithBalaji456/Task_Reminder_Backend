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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Value("${app.scheduling.late-window-minutes:60}")
    private int lateWindowMinutes;

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

    public void processTaskReminders() {
        Instant now = Instant.now();
        ZoneId serverZone = ZoneId.systemDefault();

        log.info("[REMINDER-SCHEDULER] START | Server time: {} | Server Zone: {} | UTC Instant: {}",
                LocalDateTime.now(serverZone), serverZone, now);

        // 1. Transactionally discover and enqueue due reminders in isolated transaction
        enqueueDueReminders(now);

        // 2. Fetch pending notifications ready for dispatch in isolated transaction
        List<Long> pendingIds = getPendingNotificationIds(now);
        log.info("[REMINDER-SCHEDULER] Discovered {} pending reminder notification(s) due for dispatch at {}", pendingIds.size(), now);

        // 3. Process each notification independently with explicit claim + dispatch
        for (Long notificationId : pendingIds) {
            try {
                processSingleNotification(notificationId);
            } catch (Exception e) {
                log.error("[REMINDER-SCHEDULER] Error processing notification ID {}: {}", notificationId, e.getMessage(), e);
            }
        }

        log.info("[REMINDER-SCHEDULER] END");
    }

    public void enqueueDueReminders(Instant now) {
        transactionTemplate.executeWithoutResult(status -> {
            List<TaskOccurrence> occurrencesToRemind = taskOccurrenceRepository.findAll().stream()
                    .filter(o -> o.getStatus() == TaskStatus.PENDING)
                    .filter(o -> o.getReminderScheduledAt() != null && !o.getReminderScheduledAt().isAfter(now))
                    .toList();

            Instant lateCutoff = now.minus(Duration.ofMinutes(lateWindowMinutes));

            for (TaskOccurrence occurrence : occurrencesToRemind) {
                User user = occurrence.getUser();
                UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);
                
                boolean pushEnabled = (preference == null || preference.isPushNotificationEnabled())
                        && (occurrence.getNotifyByPush() == null || occurrence.getNotifyByPush());
                boolean emailEnabled = (preference == null || preference.isTaskReminderEnabled())
                        && (occurrence.getNotifyByEmail() == null || occurrence.getNotifyByEmail());

                if (!emailEnabled && !pushEnabled) {
                    log.info("[REMINDER-SCHEDULER] SKIPPED occurrence ID {} because all notification channels are disabled for user ID {}", occurrence.getId(), user.getId());
                    continue;
                }

                Instant scheduledTime = occurrence.getReminderScheduledAt();
                if (scheduledTime.isBefore(lateCutoff)) {
                    log.warn("[REMINDER-SCHEDULER] SKIPPED occurrence ID {} for taskId={} because scheduled reminder time {} is past late processing window of {} mins",
                            occurrence.getId(), occurrence.getId(), scheduledTime, lateWindowMinutes);
                    Integer repeatFreq = occurrence.getRepeatFrequencyMinutes();
                    if (repeatFreq != null && repeatFreq > 0) {
                        Instant next = scheduledTime.plus(Duration.ofMinutes(repeatFreq));
                        occurrence.setReminderScheduledAt(next.isBefore(occurrence.getDueDateTime()) ? next : null);
                    } else {
                        occurrence.setReminderScheduledAt(null);
                    }
                    taskOccurrenceRepository.save(occurrence);
                    continue;
                }

                boolean alreadyDelivered = emailNotificationRepository.existsByTaskOccurrenceIdAndNotificationTypeAndScheduledFor(
                        occurrence.getId(), NotificationType.TASK_REMINDER, scheduledTime
                );

                log.info("[REMINDER-SCHEDULER]\ntaskId={}\ndueTime={}\nfirstReminder={}\ninterval={} minutes\nnow={}\noccurrence={}\nalreadyDelivered={}\npushEnabled={}\nemailEnabled={}",
                        occurrence.getId(),
                        occurrence.getDueTime(),
                        occurrence.getReminderOption(),
                        occurrence.getRepeatFrequencyMinutes() != null ? occurrence.getRepeatFrequencyMinutes() : 0,
                        now,
                        scheduledTime,
                        alreadyDelivered,
                        pushEnabled,
                        emailEnabled
                );

                if (alreadyDelivered) {
                    log.info("[REMINDER-SCHEDULER] Skipping occurrence {} for taskId={} because this exact occurrence already exists with SENT status.", scheduledTime, occurrence.getId());
                    continue;
                }

                EmailNotification notification = EmailNotification.builder()
                        .user(user)
                        .taskOccurrence(occurrence)
                        .notificationType(NotificationType.TASK_REMINDER)
                        .scheduledFor(scheduledTime)
                        .status(NotificationStatus.PENDING)
                        .attemptCount(0)
                        .maxAttempts(3)
                        .build();

                emailNotificationRepository.save(notification);
                log.info("[REMINDER-SCHEDULER] ENQUEUED PENDING reminder notification ID {} for task occurrence ID '{}' (scheduledFor: {}, due at {})",
                        notification.getId(), occurrence.getTitle(), scheduledTime, occurrence.getDueDateTime());
            }
            emailNotificationRepository.flush();
        });
    }

    public List<Long> getPendingNotificationIds(Instant now) {
        return transactionTemplate.execute(status ->
            emailNotificationRepository.findPendingNotificationsDue(NotificationStatus.PENDING, now)
                    .stream()
                    .map(EmailNotification::getId)
                    .toList()
        );
    }

    public void processSingleNotification(Long notificationId) {
        log.info("[REMINDER-DISPATCHER] Attempting atomic claim for notification ID: {}", notificationId);
        
        // Step A: Atomically claim notification in an isolated transaction
        ClaimedNotification claimed = claimNotification(notificationId);
        if (claimed == null) {
            log.info("[REMINDER-DISPATCHER] Notification ID {} skipped (already claimed or task not pending)", notificationId);
            return;
        }

        log.info("[REMINDER-DISPATCHER] Successfully claimed notification ID: {} | User: {} | Task: '{}' | Attempt #{}",
                notificationId, claimed.userEmail, claimed.taskTitle, claimed.attemptCount);

        boolean emailSuccess = false;
        boolean pushSuccess = false;
        String providerMsgId = null;
        String failureMessage = null;

        // Step B: External Network Calls (OUTSIDE DB TRANSACTION BOUNDARY)
        try {
            // Channel 1: Email via Brevo API
            if (claimed.isEmailEnabled) {
                log.info("[REMINDER-DISPATCHER] [EMAIL] Dispatching Brevo email for notification ID {} to {}", notificationId, claimed.userEmail);
                try {
                    providerMsgId = emailSenderService.sendEmail(
                            claimed.userEmail, claimed.userName,
                            "⏰ Task Reminder: " + claimed.taskTitle,
                            claimed.emailHtml
                    );
                    emailSuccess = true;
                    log.info("[REMINDER-DISPATCHER] [EMAIL] Brevo email delivered successfully. MsgId: {}", providerMsgId);
                } catch (Exception ex) {
                    log.error("[REMINDER-DISPATCHER] [EMAIL] Brevo delivery failed for notification ID {}: {}", notificationId, ex.getMessage(), ex);
                    failureMessage = "Email error: " + ex.getMessage();
                }
            } else {
                log.info("[REMINDER-DISPATCHER] [EMAIL] Skipped (channel disabled in user preferences)");
            }

            // Channel 2: Web Push Notifications
            if (claimed.isPushEnabled && claimed.activeSubscriptions != null && !claimed.activeSubscriptions.isEmpty()) {
                log.info("[REMINDER-DISPATCHER] [PUSH] Found {} active Web Push subscription(s) for user {}", claimed.activeSubscriptions.size(), claimed.userName);
                for (PushSubscription sub : claimed.activeSubscriptions) {
                    try {
                        log.info("[REMINDER-DISPATCHER] [PUSH] Dispatching VAPID Web Push to endpoint: {}", sub.getEndpoint());
                        boolean ok = webPushService.sendPushNotification(sub, claimed.pushTitle, claimed.pushBody, "/tasks", claimed.occurrenceId);
                        if (ok) {
                            pushSuccess = true;
                            log.info("[REMINDER-DISPATCHER] [PUSH] VAPID Web Push delivered successfully");
                        }
                    } catch (Exception ex) {
                        log.error("[REMINDER-DISPATCHER] [PUSH] Web Push delivery failed for endpoint {}: {}", sub.getEndpoint(), ex.getMessage(), ex);
                    }
                }
            } else {
                log.info("[REMINDER-DISPATCHER] [PUSH] Skipped (channel disabled or no active device subscriptions registered)");
            }
        } catch (Exception ex) {
            failureMessage = ex.getMessage();
        }

        // Step C: Record final result in an isolated transaction
        boolean overallSuccess = emailSuccess || pushSuccess || (!claimed.isEmailEnabled && !claimed.isPushEnabled);
        recordNotificationResult(notificationId, overallSuccess, providerMsgId != null ? providerMsgId : "MULTI_CHANNEL_SENT", failureMessage);
        log.info("[REMINDER-DISPATCHER] Notification ID {} completed processing with final status: {}", notificationId, overallSuccess ? "SENT" : "FAILED");
    }

    public ClaimedNotification claimNotification(Long notificationId) {
        return transactionTemplate.execute(status -> {
            Optional<EmailNotification> opt = emailNotificationRepository.findById(notificationId);
            if (opt.isEmpty()) return null;

            EmailNotification notification = opt.get();
            TaskOccurrence occurrence = notification.getTaskOccurrence();

            if (occurrence != null && occurrence.getStatus() != TaskStatus.PENDING) {
                notification.setStatus(NotificationStatus.CANCELLED);
                notification.setFailureReason("Task occurrence is " + occurrence.getStatus());
                emailNotificationRepository.saveAndFlush(notification);
                return null;
            }

            if (notification.getStatus() != NotificationStatus.PENDING) {
                return null;
            }

            int newAttemptCount = notification.getAttemptCount() + 1;
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.setAttemptCount(newAttemptCount);
            emailNotificationRepository.saveAndFlush(notification);

            User user = notification.getUser();
            UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);

            boolean isEmailEnabled = (preference == null || preference.isTaskReminderEnabled())
                    && (occurrence == null || occurrence.getNotifyByEmail() == null || occurrence.getNotifyByEmail());
            boolean isPushEnabled = (preference == null || preference.isPushNotificationEnabled())
                    && (occurrence == null || occurrence.getNotifyByPush() == null || occurrence.getNotifyByPush());

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
                    occurrence.getId(),
                    user.getEmail(),
                    user.getName(),
                    occurrence.getTitle(),
                    html,
                    pushTitle,
                    pushBody,
                    isEmailEnabled,
                    isPushEnabled,
                    activeSubs,
                    newAttemptCount
            );
        });
    }

    public void recordNotificationResult(Long notificationId, boolean success, String msgId, String failureReason) {
        transactionTemplate.executeWithoutResult(status -> {
            Optional<EmailNotification> opt = emailNotificationRepository.findById(notificationId);
            if (opt.isEmpty()) return;

            EmailNotification notification = opt.get();
            TaskOccurrence occurrence = notification.getTaskOccurrence();

            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setProviderMessageId(msgId);

                // Handle repeat reminder frequency scheduling
                if (occurrence != null && occurrence.getStatus() == TaskStatus.PENDING) {
                    int sentCount = (occurrence.getReminderSentCount() != null ? occurrence.getReminderSentCount() : 0) + 1;
                    occurrence.setReminderSentCount(sentCount);

                    Integer repeatFreq = occurrence.getRepeatFrequencyMinutes();
                    String stopCondition = occurrence.getRepeatStopCondition() != null ? occurrence.getRepeatStopCondition() : "UNTIL_TASK_TIME";
                    Integer maxCount = occurrence.getMaxReminderCount() != null ? occurrence.getMaxReminderCount() : 5;

                    boolean shouldRepeat = repeatFreq != null && repeatFreq > 0;
                    if ("AFTER_MAX_COUNT".equalsIgnoreCase(stopCondition) && sentCount >= maxCount) {
                        shouldRepeat = false;
                    }

                    if (shouldRepeat) {
                        Instant baseTime = notification.getScheduledFor() != null ? notification.getScheduledFor() : (occurrence.getReminderScheduledAt() != null ? occurrence.getReminderScheduledAt() : Instant.now());
                        Instant nextReminderAt = baseTime.plus(Duration.ofMinutes(repeatFreq));
                        if (nextReminderAt.isBefore(occurrence.getDueDateTime())) {
                            occurrence.setReminderScheduledAt(nextReminderAt);
                            taskOccurrenceRepository.save(occurrence);
                            log.info("[REMINDER-SCHEDULER] SCHEDULED REPEAT REMINDER #{} for task occurrence ID {} at {}",
                                    sentCount + 1, occurrence.getId(), nextReminderAt);
                        } else {
                            occurrence.setReminderScheduledAt(null);
                            taskOccurrenceRepository.save(occurrence);
                            log.info("[REMINDER-SCHEDULER] REPEAT REMINDERS ENDED for task occurrence ID {} (next reminder {} would reach or exceed due time {})", occurrence.getId(), nextReminderAt, occurrence.getDueDateTime());
                        }
                    } else {
                        occurrence.setReminderScheduledAt(null);
                        taskOccurrenceRepository.save(occurrence);
                        log.info("[REMINDER-SCHEDULER] REPEAT REMINDERS COMPLETED for task occurrence ID {} (sent count: {})", occurrence.getId(), sentCount);
                    }
                }
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
            emailNotificationRepository.saveAndFlush(notification);
        });
    }

    public record ClaimedNotification(
            Long notificationId,
            Long occurrenceId,
            String userEmail,
            String userName,
            String taskTitle,
            String emailHtml,
            String pushTitle,
            String pushBody,
            boolean isEmailEnabled,
            boolean isPushEnabled,
            List<PushSubscription> activeSubscriptions,
            int attemptCount
    ) {}
}
