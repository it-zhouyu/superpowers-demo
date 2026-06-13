package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.SendCodeRequest;
import com.example.auth.service.AuthService;
import com.example.auth.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @MockBean
    private AuthService authService;

    @Test
    void sendCode_shouldReturn200_whenEmailValid() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码发送成功"));

        verify(emailService).sendCode("test@example.com");
    }

    @Test
    void sendCode_shouldReturn400_whenEmailInvalid() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
    }

    @Test
    void sendCode_shouldReturn400_whenEmailEmpty() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setEmail("");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱不能为空"));
    }

    @Test
    void login_shouldReturn200_whenSuccess() throws Exception {
        LoginResponse mockResponse = LoginResponse.builder()
                .token("test-token")
                .userId(1L)
                .email("test@example.com")
                .nickname("用户123456")
                .build();
        when(authService.loginOrRegister(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_shouldReturn400_whenCodeInvalid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setCode("abc");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("验证码格式不正确"));
    }

    @Test
    void me_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
