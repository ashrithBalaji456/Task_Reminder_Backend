package com.application.taskmanager.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailPreferenceRequest {

    @NotNull(message = "taskReminderEnabled field is required")
    private Boolean taskReminderEnabled;

    private Boolean pushNotificationEnabled;

    @NotNull(message = "weeklyReportEnabled field is required")
    private Boolean weeklyReportEnabled;

    @NotNull(message = "monthlyReportEnabled field is required")
    private Boolean monthlyReportEnabled;

    private String preferredWeeklyReportDay;

    private LocalTime preferredWeeklyReportTime;

    private String timezone;
}
