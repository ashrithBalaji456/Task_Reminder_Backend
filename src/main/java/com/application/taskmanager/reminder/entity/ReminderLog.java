package com.application.taskmanager.reminder.entity;

import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reminder_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_occurrence_id", nullable = false)
    private TaskOccurrence taskOccurrence;

    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "brevo_message_id", length = 100)
    private String brevoMessageId;
}
