package com.application.taskmanager.auth.controller;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.LoginRequest;
import com.application.taskmanager.auth.dto.RefreshTokenRequest;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user credentials and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT access token", description = "Generates a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes refresh token for user")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a reset token and dispatches an email via Brevo")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody com.application.taskmanager.auth.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If an account exists with that email, a password reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using a valid reset token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody com.application.taskmanager.auth.dto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. You can now log in.", null));
    }
}
