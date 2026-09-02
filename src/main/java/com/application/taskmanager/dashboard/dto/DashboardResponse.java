package com.application.taskmanager.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private LocalDate date;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long cancelledTasks;
    private long highPriorityPendingCount;
    private long mediumPriorityPendingCount;
    private long lowPriorityPendingCount;
    private double completionPercentage;
}
