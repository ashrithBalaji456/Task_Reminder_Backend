package com.application.taskmanager.task.dto;

import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.ReminderOption;
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
public class UpdateTaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LocalDate dueDate;

    @NotNull(message = "Due time is required")
    private LocalTime dueTime;

    private ReminderOption reminderOption;

    private Integer customReminderMinutes;

    private Integer repeatFrequencyMinutes;

    private String repeatStopCondition;

    private Integer maxReminderCount;

    private Boolean notifyByEmail;

    private Boolean notifyByPush;

    private Boolean recurring;
}
