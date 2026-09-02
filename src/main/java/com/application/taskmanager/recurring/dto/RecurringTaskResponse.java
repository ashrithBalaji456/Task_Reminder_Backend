package com.application.taskmanager.recurring.dto;

import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.RecurrenceType;
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
public class RecurringTaskResponse {

    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private TaskType taskType;
    private RecurrenceType recurrenceType;
    private LocalDate startDate;
    private LocalTime dueTime;
    private boolean locked;
    private Instant createdAt;
    private Instant updatedAt;
}
