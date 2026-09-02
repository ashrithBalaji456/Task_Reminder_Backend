package com.application.taskmanager.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "System Health", description = "Endpoint for checking service health, database connectivity, and uptime")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "Check Application Health", description = "Returns application status, database connectivity, and timestamp")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "task-reminder-backend");
        health.put("timestamp", Instant.now().toString());

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN (" + e.getMessage() + ")");
            health.put("status", "DEGRADED");
            return ResponseEntity.status(503).body(health);
        }

        return ResponseEntity.ok(health);
    }
}
