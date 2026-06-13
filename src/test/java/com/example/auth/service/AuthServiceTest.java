package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.BusinessException;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=";
        jwtService = new JwtService(testSecret, 3600000);
        authService = new AuthService(userRepository, emailService, jwtService);
    }

    @Test
    void loginOrRegister_shouldCreateNewUser_whenEmailNotExists() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        LoginResponse response = authService.loginOrRegister(request);

        assertNotNull(response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getNickname().startsWith("用户"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginOrRegister_shouldReturnExistingUser_whenEmailExists() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        User existingUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("已有用户")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        LoginResponse response = authService.loginOrRegister(request);

        assertNotNull(response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("已有用户", response.getNickname());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginOrRegister_shouldCallEmailVerify() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        authService.loginOrRegister(request);
        verify(emailService).verifyCode("test@example.com", "123456");
    }

    @Test
    void loginOrRegister_shouldThrow_whenEmailVerifyFails() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("000000");

        doThrow(new BusinessException("验证码错误"))
                .when(emailService).verifyCode(anyString(), anyString());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.loginOrRegister(request));
        assertEquals("验证码错误", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }
}
