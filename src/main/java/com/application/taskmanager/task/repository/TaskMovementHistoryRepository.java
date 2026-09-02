package com.application.taskmanager.task.repository;

import com.application.taskmanager.task.entity.TaskMovementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskMovementHistoryRepository extends JpaRepository<TaskMovementHistory, Long> {

    List<TaskMovementHistory> findByUserIdAndTaskOccurrenceId(Long userId, Long taskOccurrenceId);
}
