package com.application.taskmanager.recurring.service;

import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.recurring.dto.RecurringTaskResponse;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.UpdateTaskRequest;
import com.application.taskmanager.task.entity.RecurrenceType;
import com.application.taskmanager.task.entity.TaskDefinition;
import com.application.taskmanager.task.entity.TaskType;
import com.application.taskmanager.task.repository.TaskDefinitionRepository;
import com.application.taskmanager.task.service.TaskMapper;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringTaskService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public RecurringTaskResponse createRecurringTask(Long userId, CreateTaskRequest request) {
        User user = getUserOrThrow(userId);

        TaskDefinition definition = TaskDefinition.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .taskType(TaskType.DAILY_RECURRING)
                .recurrenceType(RecurrenceType.DAILY)
                .startDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now())
                .dueTime(request.getDueTime())
                .locked(false)
                .build();

        TaskDefinition saved = taskDefinitionRepository.save(definition);
        return taskMapper.toRecurringTaskResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringTaskResponse> getRecurringTasks(Long userId) {
        List<TaskDefinition> definitions = taskDefinitionRepository
                .findByUserIdAndTaskType(userId, TaskType.DAILY_RECURRING);
        return definitions.stream().map(taskMapper::toRecurringTaskResponse).collect(Collectors.toList());
    }

    @Transactional
    public RecurringTaskResponse updateRecurringTask(Long userId, Long recurringTaskId, UpdateTaskRequest request) {
        TaskDefinition definition = taskDefinitionRepository.findByIdAndUserId(recurringTaskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring task definition not found with id: " + recurringTaskId));

        definition.setTitle(request.getTitle());
        definition.setDescription(request.getDescription());
        definition.setPriority(request.getPriority());
        definition.setDueTime(request.getDueTime());
        if (request.getDueDate() != null) {
            definition.setStartDate(request.getDueDate());
        }

        TaskDefinition updated = taskDefinitionRepository.save(definition);
        return taskMapper.toRecurringTaskResponse(updated);
    }

    @Transactional
    public RecurringTaskResponse lockRecurringTask(Long userId, Long recurringTaskId) {
        TaskDefinition definition = taskDefinitionRepository.findByIdAndUserId(recurringTaskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring task definition not found with id: " + recurringTaskId));

        definition.setLocked(true);
        TaskDefinition updated = taskDefinitionRepository.save(definition);
        log.info("Locked recurring task definition id {}", recurringTaskId);
        return taskMapper.toRecurringTaskResponse(updated);
    }

    @Transactional
    public RecurringTaskResponse unlockRecurringTask(Long userId, Long recurringTaskId) {
        TaskDefinition definition = taskDefinitionRepository.findByIdAndUserId(recurringTaskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring task definition not found with id: " + recurringTaskId));

        definition.setLocked(false);
        TaskDefinition updated = taskDefinitionRepository.save(definition);
        log.info("Unlocked recurring task definition id {}", recurringTaskId);
        return taskMapper.toRecurringTaskResponse(updated);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
