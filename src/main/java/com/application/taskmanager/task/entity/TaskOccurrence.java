package com.application.taskmanager.task.entity;

import com.application.taskmanager.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "task_occurrences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_definition_id")
    private TaskDefinition taskDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Column(name = "due_time", nullable = false)
    private LocalTime dueTime;

    @Column(name = "due_date_time", nullable = false)
    private Instant dueDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_option", nullable = false, length = 30)
    @Builder.Default
    private ReminderOption reminderOption = ReminderOption.NONE;

    @Column(name = "custom_reminder_minutes")
    private Integer customReminderMinutes;

    @Column(name = "reminder_scheduled_at")
    private Instant reminderScheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
