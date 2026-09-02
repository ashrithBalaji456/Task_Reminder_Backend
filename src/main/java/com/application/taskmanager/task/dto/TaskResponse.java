package com.application.taskmanager.task.dto;

import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.RecurrenceType;
import com.application.taskmanager.task.entity.ReminderOption;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.entity.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private Long taskDefinitionId;
    private String title;
    private String description;
    private Priority priority;
    private TaskType taskType;
    private RecurrenceType recurrenceType;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Instant dueDateTime;
    private ReminderOption reminderOption;
    private Integer customReminderMinutes;
    private Instant reminderScheduledAt;
    private TaskStatus status;
    private boolean recurring;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
