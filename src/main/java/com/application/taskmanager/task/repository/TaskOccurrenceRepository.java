package com.application.taskmanager.task.repository;

import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskOccurrenceRepository extends JpaRepository<TaskOccurrence, Long>, JpaSpecificationExecutor<TaskOccurrence> {

    Optional<TaskOccurrence> findByIdAndUserId(Long id, Long userId);

    Optional<TaskOccurrence> findByUserIdAndTaskDefinitionIdAndOccurrenceDate(Long userId, Long taskDefinitionId, LocalDate occurrenceDate);

    boolean existsByUserIdAndTaskDefinitionIdAndOccurrenceDate(Long userId, Long taskDefinitionId, LocalDate occurrenceDate);

    List<TaskOccurrence> findByUserIdAndOccurrenceDate(Long userId, LocalDate occurrenceDate);

    List<TaskOccurrence> findByUserIdAndOccurrenceDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<TaskOccurrence> findByUserIdAndStatus(Long userId, TaskStatus status);

    @Query("SELECT o FROM TaskOccurrence o WHERE o.user.id = :userId AND o.status = :status AND o.dueDateTime >= :now AND o.dueDateTime <= :dueWindowEnd ORDER BY o.priority ASC, o.dueTime ASC")
    List<TaskOccurrence> findAlertsForUser(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("now") Instant now,
            @Param("dueWindowEnd") Instant dueWindowEnd
    );

    @Query("SELECT o FROM TaskOccurrence o WHERE o.status = :status AND o.dueDateTime >= :startWindow AND o.dueDateTime <= :endWindow")
    List<TaskOccurrence> findPendingTasksDueWithinWindow(
            @Param("status") TaskStatus status,
            @Param("startWindow") Instant startWindow,
            @Param("endWindow") Instant endWindow
    );

    @Query("SELECT o FROM TaskOccurrence o WHERE o.user.id = :userId AND o.occurrenceDate = :date ORDER BY CASE o.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END ASC, o.dueTime ASC")
    List<TaskOccurrence> findByUserIdAndOccurrenceDateOrdered(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    long countByUserIdAndOccurrenceDate(Long userId, LocalDate date);

    long countByUserIdAndOccurrenceDateAndStatus(Long userId, LocalDate date, TaskStatus status);

    long countByUserIdAndOccurrenceDateAndStatusAndPriority(Long userId, LocalDate date, TaskStatus status, Priority priority);

    @Query("SELECT o FROM TaskOccurrence o WHERE o.user.id = :userId ORDER BY o.occurrenceDate DESC, o.dueTime DESC")
    List<TaskOccurrence> findAllByUserIdOrdered(@Param("userId") Long userId);
}
