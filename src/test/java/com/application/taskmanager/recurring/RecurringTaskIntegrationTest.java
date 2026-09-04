package com.application.taskmanager.recurring;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.history.dto.DailyHistoryResponse;
import com.application.taskmanager.history.service.HistoryService;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecurringTaskIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    private Long userId;

    @BeforeEach
    void setUp() {
        RegisterRequest register = RegisterRequest.builder()
                .name("Test User")
                .email("testuser@example.com")
                .password("Password123!")
                .timezone(java.time.ZoneId.systemDefault().getId())
                .build();
        AuthResponse auth = authService.register(register);
        userId = auth.getUserId();
    }

    @Test
    @DisplayName("Completing today's occurrence of a recurring task does NOT affect tomorrow's occurrence")
    void testRecurringTask_CompletionIsolationAcrossDays() {
        java.time.ZoneId userZone = java.time.ZoneId.systemDefault();
        LocalDate today = LocalDate.now(userZone);
        LocalDate tomorrow = today.plusDays(1);

        // 1. Create a daily recurring task
        CreateTaskRequest createRequest = CreateTaskRequest.builder()
                .title("Take Medicine")
                .description("Daily vitamin intake")
                .priority(Priority.HIGH)
                .dueDate(today)
                .dueTime(LocalTime.now(userZone).plusHours(2))
                .recurring(true)
                .build();

        TaskResponse todayTask = taskService.createTask(userId, createRequest);
        assertNotNull(todayTask);
        assertEquals(today, todayTask.getDueDate());
        assertEquals(TaskStatus.PENDING, todayTask.getStatus());

        // 2. Complete today's occurrence
        TaskResponse completedToday = taskService.completeTask(userId, todayTask.getId());
        assertEquals(TaskStatus.COMPLETED, completedToday.getStatus());
        assertNotNull(completedToday.getCompletedAt());

        // 3. Fetch tomorrow's tasks (triggers occurrence materialization)
        List<TaskResponse> tomorrowTasks = taskService.getTomorrowTasks(userId);
        assertFalse(tomorrowTasks.isEmpty());

        TaskResponse tomorrowOccurrence = tomorrowTasks.stream()
                .filter(t -> t.getTitle().equals("Take Medicine"))
                .findFirst()
                .orElse(null);

        assertNotNull(tomorrowOccurrence);
        assertEquals(tomorrow, tomorrowOccurrence.getDueDate());
        // CRITICAL CHECK: Tomorrow's occurrence must remain PENDING
        assertEquals(TaskStatus.PENDING, tomorrowOccurrence.getStatus());
        assertNull(tomorrowOccurrence.getCompletedAt());

        // 4. Verify historical record for today shows COMPLETED
        DailyHistoryResponse todayHistory = historyService.getDailyHistory(userId, today);
        assertEquals(1, todayHistory.getCompletedTasks());
        assertEquals(100.0, todayHistory.getCompletionPercentage());

        // 5. Verify historical record for tomorrow shows PENDING
        DailyHistoryResponse tomorrowHistory = historyService.getDailyHistory(userId, tomorrow);
        assertEquals(0, tomorrowHistory.getCompletedTasks());
        assertEquals(1, tomorrowHistory.getPendingTasks());
        assertEquals(0.0, tomorrowHistory.getCompletionPercentage());
    }
}
