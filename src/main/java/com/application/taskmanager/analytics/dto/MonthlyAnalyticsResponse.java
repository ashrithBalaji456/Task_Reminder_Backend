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
public class MonthlyAnalyticsResponse {

    private String monthName; // e.g. "August 2026"
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long cancelledTasks;
    private long movedTasks;
    private double completionRate;

    // Priority-wise breakdown
    private long highPriorityCompleted;
    private long highPriorityPending;
    private long mediumPriorityCompleted;
    private long mediumPriorityPending;
    private long lowPriorityCompleted;
    private long lowPriorityPending;

    private String mostProductiveDay;
    private String bestWeek;

    // Previous month comparison
    private double previousMonthCompletionRate;
    private double completionRateDifference;
    private String comparisonMessage;

    // Daily & weekly maps for charts
    private Map<LocalDate, Long> dailyCompletedMap;
}
