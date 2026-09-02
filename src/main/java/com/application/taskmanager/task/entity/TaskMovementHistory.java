package com.application.taskmanager.task.entity;

import com.application.taskmanager.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "task_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskMovementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_occurrence_id", nullable = false)
    private TaskOccurrence taskOccurrence;

    @Column(name = "source_date", nullable = false)
    private LocalDate sourceDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @CreationTimestamp
    @Column(name = "moved_at", nullable = false, updatable = false)
    private Instant movedAt;
}
