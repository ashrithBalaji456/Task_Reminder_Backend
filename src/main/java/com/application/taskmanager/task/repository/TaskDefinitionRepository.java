package com.application.taskmanager.task.repository;

import com.application.taskmanager.task.entity.TaskDefinition;
import com.application.taskmanager.task.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Long> {

    Optional<TaskDefinition> findByIdAndUserId(Long id, Long userId);

    List<TaskDefinition> findByUserId(Long userId);

    List<TaskDefinition> findByUserIdAndTaskType(Long userId, TaskType taskType);

    List<TaskDefinition> findByUserIdAndTaskTypeAndStartDateLessThanEqual(Long userId, TaskType taskType, LocalDate date);

    List<TaskDefinition> findByTaskTypeAndStartDateLessThanEqual(TaskType taskType, LocalDate date);
}
