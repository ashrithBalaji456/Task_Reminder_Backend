package com.application.taskmanager.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "user_email_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEmailPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "task_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean taskReminderEnabled = true;

    @Column(name = "push_notification_enabled", nullable = false)
    @Builder.Default
    private boolean pushNotificationEnabled = true;

    @Column(name = "weekly_report_enabled", nullable = false)
    @Builder.Default
    private boolean weeklyReportEnabled = true;

    @Column(name = "monthly_report_enabled", nullable = false)
    @Builder.Default
    private boolean monthlyReportEnabled = true;

    @Column(name = "preferred_weekly_report_day", nullable = false, length = 20)
    @Builder.Default
    private String preferredWeeklyReportDay = "SUNDAY";

    @Column(name = "preferred_weekly_report_time", nullable = false)
    @Builder.Default
    private LocalTime preferredWeeklyReportTime = LocalTime.of(18, 0);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
