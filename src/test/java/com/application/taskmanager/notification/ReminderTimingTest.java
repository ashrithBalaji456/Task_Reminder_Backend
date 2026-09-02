package com.application.taskmanager.notification;

import com.application.taskmanager.task.entity.ReminderOption;
import com.application.taskmanager.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReminderTimingTest {

    private final TaskService taskService = new TaskService(null, null, null, null, null);

    @Test
    @DisplayName("Should correctly calculate reminder scheduled time for 10_MINUTES")
    void testTenMinutesReminder() {
        Instant dueDateTime = Instant.parse("2026-09-03T10:00:00Z");
        Instant scheduledAt = taskService.calculateReminderScheduledAt(dueDateTime, ReminderOption.TEN_MINUTES, null);

        assertNotNull(scheduledAt);
        assertEquals(Duration.ofMinutes(10), Duration.between(scheduledAt, dueDateTime));
        assertEquals(Instant.parse("2026-09-03T09:50:00Z"), scheduledAt);
    }

    @Test
    @DisplayName("Should correctly calculate reminder scheduled time for 30_MINUTES")
    void testThirtyMinutesReminder() {
        Instant dueDateTime = Instant.parse("2026-09-03T10:00:00Z");
        Instant scheduledAt = taskService.calculateReminderScheduledAt(dueDateTime, ReminderOption.THIRTY_MINUTES, null);

        assertNotNull(scheduledAt);
        assertEquals(Instant.parse("2026-09-03T09:30:00Z"), scheduledAt);
    }

    @Test
    @DisplayName("Should correctly calculate reminder scheduled time for 1_HOUR")
    void testOneHourReminder() {
        Instant dueDateTime = Instant.parse("2026-09-03T10:00:00Z");
        Instant scheduledAt = taskService.calculateReminderScheduledAt(dueDateTime, ReminderOption.ONE_HOUR, null);

        assertNotNull(scheduledAt);
        assertEquals(Instant.parse("2026-09-03T09:00:00Z"), scheduledAt);
    }

    @Test
    @DisplayName("Should correctly calculate reminder scheduled time for CUSTOM option")
    void testCustomReminder() {
        Instant dueDateTime = Instant.parse("2026-09-03T10:00:00Z");
        Instant scheduledAt = taskService.calculateReminderScheduledAt(dueDateTime, ReminderOption.CUSTOM, 45);

        assertNotNull(scheduledAt);
        assertEquals(Instant.parse("2026-09-03T09:15:00Z"), scheduledAt);
    }

    @Test
    @DisplayName("Should return null when reminder option is NONE")
    void testNoneReminder() {
        Instant dueDateTime = Instant.parse("2026-09-03T10:00:00Z");
        Instant scheduledAt = taskService.calculateReminderScheduledAt(dueDateTime, ReminderOption.NONE, null);

        assertNull(scheduledAt);
    }
}
