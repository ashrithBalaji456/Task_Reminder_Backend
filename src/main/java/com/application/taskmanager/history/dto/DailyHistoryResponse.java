package com.application.taskmanager.history.dto;

import com.application.taskmanager.task.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHistoryResponse {

    private LocalDate date;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long cancelledTasks;
    private double completionPercentage;
    private List<TaskResponse> tasks;
}
