package com.application.taskmanager.notification;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.notification.client.BrevoEmailClient;
import com.application.taskmanager.notification.entity.EmailNotification;
import com.application.taskmanager.notification.entity.NotificationStatus;
import com.application.taskmanager.notification.repository.EmailNotificationRepository;
import com.application.taskmanager.notification.scheduler.TaskReminderScheduler;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.ReminderOption;
import com.application.taskmanager.task.repository.TaskDefinitionRepository;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.task.service.TaskService;
import com.application.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateAndCompletedTaskRuleIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskReminderScheduler reminderScheduler;

    @Autowired
    private EmailNotificationRepository notificationRepository;

    @Autowired
    private TaskOccurrenceRepository taskOccurrenceRepository;

    @Autowired
    private TaskDefinitionRepository taskDefinitionRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private BrevoEmailClient brevoEmailClient;

    private Long userId;
    private ZoneId userZone;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        taskOccurrenceRepository.deleteAll();
        taskDefinitionRepository.deleteAll();
        userRepository.deleteAll();
        reset(brevoEmailClient);
        userZone = ZoneId.systemDefault();
        RegisterRequest register = RegisterRequest.builder()
                .name("Notification Test User")
                .email("notiftest@example.com")
                .password("Password123!")
                .timezone(userZone.getId())
                .build();
        AuthResponse auth = authService.register(register);
        userId = auth.getUserId();

        when(brevoEmailClient.sendEmail(any(), any(), any(), any(), any()))
                .thenReturn("MOCK_BREVO_MSG_123");
    }

    @Test
    @DisplayName("Running scheduler twice sends reminder email exactly ONCE (Duplicate Prevention)")
    void testDuplicatePrevention() {
        LocalDate today = LocalDate.now(userZone);
        LocalTime dueTime = LocalTime.now(userZone).plusMinutes(5);

        CreateTaskRequest create = CreateTaskRequest.builder()
                .title("Urgent Meeting")
                .priority(Priority.HIGH)
                .dueDate(today)
                .dueTime(dueTime)
                .reminderOption(ReminderOption.TEN_MINUTES)
                .build();

        TaskResponse task = taskService.createTask(userId, create);
        assertNotNull(task.getReminderScheduledAt());

        clearInvocations(brevoEmailClient);

        // First scheduler run -> should process and send email
        reminderScheduler.processTaskReminders();
        verify(brevoEmailClient, times(1)).sendEmail(any(), any(), any(), any(), any());

        // Second scheduler run -> should NOT trigger duplicate email
        reminderScheduler.processTaskReminders();
        verify(brevoEmailClient, times(1)).sendEmail(any(), any(), any(), any(), any());

        List<EmailNotification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
        assertEquals(NotificationStatus.SENT, notifications.get(0).getStatus());
    }

    @Test
    @DisplayName("Completing a task before reminder execution cancels notification without sending email")
    void testCompletedTaskRule_CancelsEmail() {
        LocalDate today = LocalDate.now(userZone);
        LocalTime dueTime = LocalTime.now(userZone).plusMinutes(5);

        CreateTaskRequest create = CreateTaskRequest.builder()
                .title("Complete Before Reminder")
                .priority(Priority.HIGH)
                .dueDate(today)
                .dueTime(dueTime)
                .reminderOption(ReminderOption.TEN_MINUTES)
                .build();

        TaskResponse task = taskService.createTask(userId, create);

        // User completes task before scheduler runs
        taskService.completeTask(userId, task.getId());

        // Scheduler runs
        reminderScheduler.processTaskReminders();

        // Brevo email client must NEVER be called
        verify(brevoEmailClient, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("All 5 repeat reminders are delivered for 2-minute interval before due time")
    void testRepeatReminders_AllOccurrencesDelivered() {
        LocalDate today = LocalDate.now(userZone);
        LocalTime dueTime = LocalTime.now(userZone).plusMinutes(10);

        CreateTaskRequest create = CreateTaskRequest.builder()
                .title("Notify Task")
                .priority(Priority.HIGH)
                .dueDate(today)
                .dueTime(dueTime)
                .reminderOption(ReminderOption.TEN_MINUTES)
                .repeatFrequencyMinutes(2)
                .repeatStopCondition("UNTIL_TASK_TIME")
                .maxReminderCount(5)
                .notifyByEmail(true)
                .notifyByPush(true)
                .build();

        TaskResponse task = taskService.createTask(userId, create);
        assertNotNull(task.getReminderScheduledAt());

        clearInvocations(brevoEmailClient);

        // Run scheduler 5 successive times, advancing occurrence scheduled time to due/now on each step
        for (int i = 0; i < 5; i++) {
            com.application.taskmanager.task.entity.TaskOccurrence occurrence = taskOccurrenceRepository.findById(task.getId()).orElseThrow();
            occurrence.setReminderScheduledAt(java.time.Instant.now().minusSeconds(1));
            taskOccurrenceRepository.saveAndFlush(occurrence);

            reminderScheduler.processTaskReminders();
        }

        // Brevo email client should be invoked exactly 5 times (once for each occurrence)
        verify(brevoEmailClient, times(5)).sendEmail(any(), any(), any(), any(), any());

        List<EmailNotification> notifications = notificationRepository.findAll();
        assertEquals(5, notifications.size());
        for (EmailNotification notif : notifications) {
            assertEquals(NotificationStatus.SENT, notif.getStatus());
        }
    }
}
