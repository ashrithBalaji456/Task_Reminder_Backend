package com.application.taskmanager.task;

import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.*;
import com.application.taskmanager.task.repository.TaskDefinitionRepository;
import com.application.taskmanager.task.repository.TaskMovementHistoryRepository;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.task.service.TaskMapper;
import com.application.taskmanager.task.service.TaskService;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskDefinitionRepository taskDefinitionRepository;

    @Mock
    private TaskOccurrenceRepository taskOccurrenceRepository;

    @Mock
    private TaskMovementHistoryRepository movementHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskService taskService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(10L)
                .name("Alice")
                .email("alice@example.com")
                .timezone("UTC")
                .build();
    }

    @Test
    @DisplayName("Should successfully create a task definition and occurrence")
    void createTask_Success() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Pay Internet Bill")
                .description("Pay via credit card")
                .priority(Priority.HIGH)
                .dueDate(LocalDate.of(2026, 9, 10))
                .dueTime(LocalTime.of(9, 0))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(taskDefinitionRepository.save(any(TaskDefinition.class))).thenAnswer(inv -> {
            TaskDefinition def = inv.getArgument(0);
            def.setId(100L);
            return def;
        });

        when(taskOccurrenceRepository.save(any(TaskOccurrence.class))).thenAnswer(inv -> {
            TaskOccurrence occ = inv.getArgument(0);
            occ.setId(200L);
            return occ;
        });

        TaskResponse response = taskService.createTask(10L, request);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(100L, response.getTaskDefinitionId());
        assertEquals("Pay Internet Bill", response.getTitle());
        assertEquals(Priority.HIGH, response.getPriority());
        assertEquals(TaskStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Should complete task occurrence")
    void completeTask_Success() {
        TaskOccurrence occurrence = TaskOccurrence.builder()
                .id(200L)
                .user(sampleUser)
                .title("Morning Run")
                .status(TaskStatus.PENDING)
                .occurrenceDate(LocalDate.now())
                .dueTime(LocalTime.of(6, 30))
                .priority(Priority.HIGH)
                .build();

        when(taskOccurrenceRepository.findByIdAndUserId(200L, 10L)).thenReturn(Optional.of(occurrence));
        when(taskOccurrenceRepository.save(any(TaskOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.completeTask(10L, 200L);

        assertNotNull(response);
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
    }
}
