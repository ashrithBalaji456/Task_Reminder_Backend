package com.application.taskmanager.alert.dto;

import com.application.taskmanager.task.entity.Priority;
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
public class AlertResponse {

    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Instant dueDateTime;
    private long minutesRemaining;
}
