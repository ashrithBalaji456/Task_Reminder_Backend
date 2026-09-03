package com.application.taskmanager.task.dto;

import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.RecurrenceType;
import com.application.taskmanager.task.entity.ReminderOption;
import com.application.taskmanager.task.entity.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private TaskType taskType;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Due time is required")
    private LocalTime dueTime;

    private boolean recurring;

    private RecurrenceType recurrenceType;

    private ReminderOption reminderOption;

    private Integer customReminderMinutes;

    private Integer repeatFrequencyMinutes;

    private String repeatStopCondition;

    private Integer maxReminderCount;

    private Boolean notifyByEmail;

    private Boolean notifyByPush;

    private String timezone;
}
