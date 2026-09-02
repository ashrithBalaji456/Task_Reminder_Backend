package com.application.taskmanager.notification.scheduler;

import com.application.taskmanager.analytics.chart.ChartGenerationService;
import com.application.taskmanager.analytics.dto.WeeklyAnalyticsResponse;
import com.application.taskmanager.analytics.service.AnalyticsService;
import com.application.taskmanager.notification.client.BrevoEmailClient.InlineAttachment;
import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.entity.NotificationType;
import com.application.taskmanager.notification.repository.EmailNotificationRepository;
import com.application.taskmanager.notification.service.EmailSenderService;
import com.application.taskmanager.notification.template.WeeklyReportEmailTemplate;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.entity.UserEmailPreference;
import com.application.taskmanager.user.repository.UserEmailPreferenceRepository;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final UserRepository userRepository;
    private final UserEmailPreferenceRepository preferenceRepository;
    private final EmailNotificationRepository notificationRepository;
    private final AnalyticsService analyticsService;
    private final ChartGenerationService chartGenerationService;
    private final EmailSenderService emailSenderService;
    private final WeeklyReportEmailTemplate weeklyReportEmailTemplate;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(cron = "${app.scheduler.weekly-report-cron:0 0 18 * * SUN}")
    @Transactional
    public void scheduledCronJob() {
        if (!schedulingEnabled) {
            return;
        }
        processWeeklyReports();
    }

    @Transactional
    public void processWeeklyReports() {
        log.info("Running scheduled weekly report generation...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                processWeeklyReportForUser(user);
            } catch (Exception ex) {
                log.error("Failed to process weekly report for user id {}: {}", user.getId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void processWeeklyReportForUser(User user) {
        processWeeklyReportForUser(user, false);
    }

    @Transactional
    public void processWeeklyReportForUser(User user, boolean force) {
        UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);
        if (!force && preference != null && !preference.isWeeklyReportEnabled()) {
            return;
        }

        ZoneId userZone = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "UTC");
        LocalDate userToday = LocalDate.now(userZone);
        LocalDate prevWeekMonday = userToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        String periodIdentifier = prevWeekMonday.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));

        boolean alreadySent = notificationRepository.existsByUserIdAndNotificationTypeAndPeriodIdentifier(
                user.getId(), NotificationType.WEEKLY_REPORT, periodIdentifier
        );

        if (!force && alreadySent) {
            return;
        }

        WeeklyAnalyticsResponse analytics = analyticsService.getWeeklyAnalytics(user.getId(), userToday);

        byte[] chartBytes = chartGenerationService.generateDailyCompletionChart(
                analytics.getDailyCompletedMap(), "Daily Completed Tasks"
        );

        InlineAttachment attachment = InlineAttachment.builder()
                .name("weekly_chart.png")
                .contentBytes(chartBytes)
                .build();

        String html = weeklyReportEmailTemplate.buildWeeklyReportHtml(user.getName(), analytics);

        String msgId = emailSenderService.sendEmailWithAttachments(
                user.getEmail(),
                user.getName(),
                "📊 Your Weekly Task Productivity Report (" + periodIdentifier + ")",
                html,
                List.of(attachment)
        );

        EmailNotification notification = EmailNotification.builder()
                .user(user)
                .notificationType(NotificationType.WEEKLY_REPORT)
                .periodIdentifier(periodIdentifier)
                .scheduledFor(Instant.now())
                .status(NotificationStatus.SENT)
                .sentAt(Instant.now())
                .providerMessageId(msgId)
                .build();

        notificationRepository.save(notification);
        log.info("Successfully sent weekly productivity report to {} for period {}", user.getEmail(), periodIdentifier);
    }
}
