package com.application.taskmanager.notification.controller;

import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.notification.entity.PushSubscription;
import com.application.taskmanager.notification.repository.PushSubscriptionRepository;
import com.application.taskmanager.notification.scheduler.MonthlyReportScheduler;
import com.application.taskmanager.notification.scheduler.WeeklyReportScheduler;
import com.application.taskmanager.notification.service.EmailSenderService;
import com.application.taskmanager.notification.service.WebPushService;
import com.application.taskmanager.notification.template.TaskReminderEmailTemplate;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/test")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final TaskReminderEmailTemplate reminderEmailTemplate;
    private final WeeklyReportScheduler weeklyReportScheduler;
    private final MonthlyReportScheduler monthlyReportScheduler;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;

    @PostMapping("/reminder")
    public ResponseEntity<?> sendTestReminderEmail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "Sample Test Task") String taskTitle
    ) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String subject = "⏰ Test Task Reminder: " + taskTitle;
        String htmlContent = reminderEmailTemplate.buildTaskReminderHtml(
                user.getName(), taskTitle, "Sample task description for test verification.", "HIGH", "Today at 09:00 AM", "30 minutes"
        );

        String messageId = emailSenderService.sendEmail(
                user.getEmail(), user.getName(), subject, htmlContent
        );

        log.info("Sent test task reminder email to {} with messageId {}", user.getEmail(), messageId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test reminder email dispatched via Brevo successfully to " + user.getEmail(),
                "messageId", messageId
        ));
    }

    @PostMapping("/push")
    public ResponseEntity<?> sendTestPushNotification(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<PushSubscription> subs = pushSubscriptionRepository.findByUserIdAndActiveTrue(user.getId());
        if (subs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No active mobile push subscriptions found for this account. Please enable Web Push in Settings first!"
            ));
        }

        int deliveredCount = 0;
        for (PushSubscription sub : subs) {
            boolean ok = webPushService.sendPushNotification(
                    sub,
                    "🌸 Test Mobile Push Alert",
                    "Hello " + user.getName() + "! Native Web Push notification delivery is working perfectly on your device.",
                    "/dashboard"
            );
            if (ok) deliveredCount++;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Dispatched VAPID Web Push alert to " + deliveredCount + " active device(s)!",
                "deliveredDevices", deliveredCount
        ));
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<?> sendTestWeeklyReport(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        weeklyReportScheduler.processWeeklyReportForUser(user, true);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Weekly productivity report email with PNG chart dispatched via Brevo to " + user.getEmail()
        ));
    }

    @PostMapping("/monthly-report")
    public ResponseEntity<?> sendTestMonthlyReport(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        monthlyReportScheduler.processMonthlyReportForUser(user, true);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Monthly productivity report email with PNG chart dispatched via Brevo to " + user.getEmail()
        ));
    }
}
