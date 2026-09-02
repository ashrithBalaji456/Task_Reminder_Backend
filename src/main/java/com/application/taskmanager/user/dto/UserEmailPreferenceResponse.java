package com.application.taskmanager.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailPreferenceResponse {

    private Long id;
    private Long userId;
    private boolean taskReminderEnabled;
    private boolean pushNotificationEnabled;
    private boolean weeklyReportEnabled;
    private boolean monthlyReportEnabled;
    private String preferredWeeklyReportDay;
    private LocalTime preferredWeeklyReportTime;
    private String timezone;
    private Instant updatedAt;
}
