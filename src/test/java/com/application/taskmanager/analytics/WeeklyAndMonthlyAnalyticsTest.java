package com.application.taskmanager.analytics;

import com.application.taskmanager.analytics.dto.MonthlyAnalyticsResponse;
import com.application.taskmanager.analytics.dto.WeeklyAnalyticsResponse;
import com.application.taskmanager.analytics.service.AnalyticsService;
import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WeeklyAndMonthlyAnalyticsTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AnalyticsService analyticsService;

    private Long userId;

    @BeforeEach
    void setUp() {
        RegisterRequest register = RegisterRequest.builder()
                .name("Analytics User")
                .email("analytics@example.com")
                .password("Password123!")
                .timezone("UTC")
                .build();
        AuthResponse auth = authService.register(register);
        userId = auth.getUserId();
    }

    @Test
    @DisplayName("Should correctly calculate weekly analytics metrics and percentage point difference")
    void testWeeklyAnalyticsCalculation() {
        LocalDate today = LocalDate.now();
        LocalDate prevWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate prevWeekTuesday = prevWeekMonday.plusDays(1);

        // Create 2 tasks for previous completed week
        CreateTaskRequest task1 = CreateTaskRequest.builder()
                .title("Task 1")
                .priority(Priority.HIGH)
                .dueDate(prevWeekMonday)
                .dueTime(LocalTime.of(9, 0))
                .build();
        TaskResponse t1 = taskService.createTask(userId, task1);
        taskService.completeTask(userId, t1.getId());

        CreateTaskRequest task2 = CreateTaskRequest.builder()
                .title("Task 2")
                .priority(Priority.MEDIUM)
                .dueDate(prevWeekTuesday)
                .dueTime(LocalTime.of(14, 0))
                .build();
        taskService.createTask(userId, task2);

        WeeklyAnalyticsResponse analytics = analyticsService.getWeeklyAnalytics(userId, today);

        assertNotNull(analytics);
        assertEquals(2, analytics.getTotalTasks());
        assertEquals(1, analytics.getCompletedTasks());
        assertEquals(1, analytics.getPendingTasks());
        assertEquals(50.0, analytics.getCompletionRate());
        assertEquals(1, analytics.getHighPriorityCompleted());
    }

    @Test
    @DisplayName("Should handle zero total tasks without division by zero errors")
    void testMonthlyAnalyticsZeroTasks() {
        LocalDate today = LocalDate.now();
        MonthlyAnalyticsResponse analytics = analyticsService.getMonthlyAnalytics(userId, today);

        assertNotNull(analytics);
        assertEquals(0, analytics.getTotalTasks());
        assertEquals(0.0, analytics.getCompletionRate());
        assertEquals(0.0, analytics.getPreviousMonthCompletionRate());
        assertEquals(0.0, analytics.getCompletionRateDifference());
    }
}
