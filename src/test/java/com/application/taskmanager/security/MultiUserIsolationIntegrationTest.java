package com.application.taskmanager.security;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiUserIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private TaskService taskService;

    private String userAToken;
    private Long userAId;

    private String userBToken;
    private Long userBId;

    private Long userATaskId;

    @BeforeEach
    void setUp() {
        // Register User A
        RegisterRequest registerA = RegisterRequest.builder()
                .name("User A")
                .email("usera@example.com")
                .password("Password123!")
                .timezone("UTC")
                .build();
        AuthResponse authA = authService.register(registerA);
        userAToken = authA.getAccessToken();
        userAId = authA.getUserId();

        // Register User B
        RegisterRequest registerB = RegisterRequest.builder()
                .name("User B")
                .email("userb@example.com")
                .password("Password123!")
                .timezone("UTC")
                .build();
        AuthResponse authB = authService.register(registerB);
        userBToken = authB.getAccessToken();
        userBId = authB.getUserId();

        // Create Task for User A
        CreateTaskRequest createTask = CreateTaskRequest.builder()
                .title("User A Confidential Task")
                .description("Secret description")
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(1))
                .dueTime(LocalTime.of(10, 0))
                .build();

        TaskResponse taskA = taskService.createTask(userAId, createTask);
        userATaskId = taskA.getId();
    }

    @Test
    @DisplayName("User A can successfully view their own task")
    void getTask_UserA_Success() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", userATaskId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userATaskId))
                .andExpect(jsonPath("$.data.title").value("User A Confidential Task"));
    }

    @Test
    @DisplayName("CRITICAL: User B attempting to access User A's task must be REJECTED (404 / Forbidden)")
    void getTask_UserB_AttemptAccess_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", userATaskId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found with id: " + userATaskId));
    }

    @Test
    @DisplayName("CRITICAL: User B attempting to complete User A's task must be REJECTED")
    void completeTask_UserB_AttemptComplete_Rejected() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", userATaskId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("CRITICAL: User B attempting to move User A's task must be REJECTED")
    void moveTask_UserB_AttemptMove_Rejected() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/{id}/move/tomorrow", userATaskId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
