package com.application.taskmanager.reminder.service;

import com.application.taskmanager.reminder.entity.ReminderLog;
import com.application.taskmanager.reminder.repository.ReminderLogRepository;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledReminderService {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final BrevoEmailService brevoEmailService;

    @Value("${app.reminder.due-soon-window-minutes:30}")
    private int dueSoonWindowMinutes;

    public static final String REMINDER_TYPE_DUE_SOON = "DUE_SOON_30_MIN";

    @Transactional
    public void processDueSoonReminders() {
        log.debug("Running scheduled task reminder check...");

        Instant now = Instant.now();
        Instant endWindow = now.plus(Duration.ofMinutes(dueSoonWindowMinutes));

        List<TaskOccurrence> dueSoonOccurrences = taskOccurrenceRepository
                .findPendingTasksDueWithinWindow(TaskStatus.PENDING, now, endWindow);

        for (TaskOccurrence occurrence : dueSoonOccurrences) {
            try {
                processOccurrenceReminder(occurrence);
            } catch (Exception ex) {
                log.error("Error sending reminder for task occurrence id {}: {}", occurrence.getId(), ex.getMessage());
            }
        }
    }

    private void processOccurrenceReminder(TaskOccurrence occurrence) {
        boolean alreadySent = reminderLogRepository
                .existsByTaskOccurrenceIdAndReminderType(occurrence.getId(), REMINDER_TYPE_DUE_SOON);

        if (alreadySent) {
            return;
        }

        User user = occurrence.getUser();
        String dueTimeStr = occurrence.getDueTime().toString() + " (" + user.getTimezone() + ")";

        try {
            String messageId = brevoEmailService.sendReminderEmail(
                    user.getEmail(),
                    user.getName(),
                    occurrence.getTitle(),
                    occurrence.getPriority().name(),
                    dueTimeStr
            );

            ReminderLog logEntry = ReminderLog.builder()
                    .user(user)
                    .taskOccurrence(occurrence)
                    .reminderType(REMINDER_TYPE_DUE_SOON)
                    .status("SENT")
                    .brevoMessageId(messageId)
                    .build();

            reminderLogRepository.save(logEntry);
            log.info("Sent idempotent reminder for occurrence id {} to user {}", occurrence.getId(), user.getEmail());

        } catch (Exception ex) {
            ReminderLog failedLog = ReminderLog.builder()
                    .user(user)
                    .taskOccurrence(occurrence)
                    .reminderType(REMINDER_TYPE_DUE_SOON)
                    .status("FAILED")
                    .build();
            reminderLogRepository.save(failedLog);
            throw ex;
        }
    }
}
