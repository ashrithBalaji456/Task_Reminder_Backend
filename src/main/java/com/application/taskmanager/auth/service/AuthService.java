package com.application.taskmanager.auth.service;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.LoginRequest;
import com.application.taskmanager.auth.dto.RefreshTokenRequest;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.entity.RefreshToken;
import com.application.taskmanager.auth.repository.RefreshTokenRepository;
import com.application.taskmanager.exception.DuplicateEmailException;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.security.JwtTokenProvider;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.application.taskmanager.reminder.service.BrevoEmailService brevoEmailService;

    @Transactional
    public void forgotPassword(com.application.taskmanager.auth.dto.ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String resetToken = java.util.UUID.randomUUID().toString();
            java.time.Instant expiry = java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES);

            user.setResetToken(resetToken);
            user.setResetTokenExpiry(expiry);
            userRepository.save(user);

            log.info("Generated password reset token for user: {}", email);
            brevoEmailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }
    }

    @Transactional
    public void resetPassword(com.application.taskmanager.auth.dto.ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.Instant.now())) {
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email address is already registered: " + request.getEmail());
        }

        String userTimezone = com.application.taskmanager.user.model.AppTimezone.normalize(request.getTimezone());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .timezone(userTimezone)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}", savedUser.getId());

        String accessToken = tokenProvider.generateTokenFromUserId(
                savedUser.getId(), savedUser.getEmail(), savedUser.getName()
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .timezone(savedUser.getTimezone())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userPrincipal.getId());

        log.info("User logged in successfully: {}", userPrincipal.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(userPrincipal.getId())
                .name(userPrincipal.getName())
                .email(userPrincipal.getEmail())
                .timezone(userPrincipal.getTimezone())
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();

        String newAccessToken = tokenProvider.generateTokenFromUserId(
                user.getId(), user.getEmail(), user.getName()
        );
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .timezone(user.getTimezone())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.deleteByToken(refreshToken);
        }
    }
}
