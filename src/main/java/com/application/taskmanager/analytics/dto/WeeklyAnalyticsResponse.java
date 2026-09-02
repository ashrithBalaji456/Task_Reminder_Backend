package com.application.taskmanager.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAnalyticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long cancelledTasks;
    private long movedTasks;
    private double completionRate;
    private long highPriorityCompleted;
    private long highPriorityPending;
    private String mostProductiveDay;
    private String leastProductiveDay;

    // Previous week comparison
    private double previousWeekCompletionRate;
    private double completionRateDifference; // e.g. +10.0 percentage points
    private String comparisonMessage;

    // Daily breakdown for chart
    private Map<LocalDate, Long> dailyCompletedMap;
}
