package com.application.taskmanager.notification.scheduler;

import com.application.taskmanager.analytics.chart.ChartGenerationService;
import com.application.taskmanager.analytics.dto.MonthlyAnalyticsResponse;
import com.application.taskmanager.analytics.service.AnalyticsService;
import com.application.taskmanager.notification.client.BrevoEmailClient.InlineAttachment;
import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.entity.NotificationType;
import com.application.taskmanager.notification.repository.EmailNotificationRepository;
import com.application.taskmanager.notification.service.EmailSenderService;
import com.application.taskmanager.notification.template.MonthlyReportEmailTemplate;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private final UserRepository userRepository;
    private final UserEmailPreferenceRepository preferenceRepository;
    private final EmailNotificationRepository notificationRepository;
    private final AnalyticsService analyticsService;
    private final ChartGenerationService chartGenerationService;
    private final EmailSenderService emailSenderService;
    private final MonthlyReportEmailTemplate monthlyReportEmailTemplate;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(cron = "${app.scheduler.monthly-report-cron:0 0 8 1 * *}")
    @Transactional
    public void scheduledCronJob() {
        if (!schedulingEnabled) {
            return;
        }
        processMonthlyReports();
    }

    @Transactional
    public void processMonthlyReports() {
        log.info("Running scheduled monthly report generation...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                processMonthlyReportForUser(user);
            } catch (Exception ex) {
                log.error("Failed to process monthly report for user id {}: {}", user.getId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void processMonthlyReportForUser(User user) {
        UserEmailPreference preference = preferenceRepository.findByUserId(user.getId()).orElse(null);
        if (preference != null && !preference.isMonthlyReportEnabled()) {
            return;
        }

        ZoneId userZone = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "UTC");
        LocalDate userToday = LocalDate.now(userZone);
        LocalDate prevMonthDate = userToday.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        String periodIdentifier = prevMonthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        boolean alreadySent = notificationRepository.existsByUserIdAndNotificationTypeAndPeriodIdentifier(
                user.getId(), NotificationType.MONTHLY_REPORT, periodIdentifier
        );

        if (alreadySent) {
            return;
        }

        MonthlyAnalyticsResponse analytics = analyticsService.getMonthlyAnalytics(user.getId(), userToday);

        byte[] chartBytes = chartGenerationService.generateDailyCompletionChart(
                analytics.getDailyCompletedMap(), "Monthly Completion Trend"
        );

        InlineAttachment attachment = InlineAttachment.builder()
                .name("monthly_chart.png")
                .contentBytes(chartBytes)
                .build();

        String html = monthlyReportEmailTemplate.buildMonthlyReportHtml(user.getName(), analytics);

        String msgId = emailSenderService.sendEmailWithAttachments(
                user.getEmail(),
                user.getName(),
                "📈 Your Monthly Task Productivity Report (" + analytics.getMonthName() + ")",
                html,
                List.of(attachment)
        );

        EmailNotification notification = EmailNotification.builder()
                .user(user)
                .notificationType(NotificationType.MONTHLY_REPORT)
                .periodIdentifier(periodIdentifier)
                .scheduledFor(Instant.now())
                .status(NotificationStatus.SENT)
                .sentAt(Instant.now())
                .providerMessageId(msgId)
                .build();

        notificationRepository.save(notification);
        log.info("Successfully sent monthly productivity report to {} for period {}", user.getEmail(), periodIdentifier);
    }
}
