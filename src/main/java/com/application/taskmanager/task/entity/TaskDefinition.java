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
@Table(name = "task_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 30)
    private RecurrenceType recurrenceType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_time", nullable = false)
    private LocalTime dueTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_option", nullable = false, length = 30)
    @Builder.Default
    private ReminderOption reminderOption = ReminderOption.NONE;

    @Column(name = "custom_reminder_minutes")
    private Integer customReminderMinutes;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
