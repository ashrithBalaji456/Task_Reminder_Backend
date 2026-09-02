package com.application.taskmanager.auth;

import com.application.taskmanager.auth.dto.AuthResponse;
import com.application.taskmanager.auth.dto.LoginRequest;
import com.application.taskmanager.auth.dto.RegisterRequest;
import com.application.taskmanager.auth.entity.RefreshToken;
import com.application.taskmanager.auth.repository.RefreshTokenRepository;
import com.application.taskmanager.auth.service.AuthService;
import com.application.taskmanager.auth.service.RefreshTokenService;
import com.application.taskmanager.exception.DuplicateEmailException;
import com.application.taskmanager.security.JwtTokenProvider;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .timezone("UTC")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("secret123")
                .timezone("UTC")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateTokenFromUserId(1L, "john@example.com", "John Doe")).thenReturn("mock-access-token");

        RefreshToken mockRefreshToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .user(sampleUser)
                .build();
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(mockRefreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when registering an existing email")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login user with valid credentials")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mock-access-token");

        RefreshToken mockRefreshToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .user(sampleUser)
                .build();
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(mockRefreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals(1L, response.getUserId());
    }
}
