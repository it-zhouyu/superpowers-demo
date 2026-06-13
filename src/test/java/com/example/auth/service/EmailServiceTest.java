package com.example.auth.service;

import com.example.auth.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
    }

    @Test
    void sendCode_shouldSuccess_whenFirstTime() {
        assertDoesNotThrow(() -> emailService.sendCode("test@example.com"));
    }

    @Test
    void sendCode_shouldThrow_whenSendTooFrequently() {
        emailService.sendCode("test@example.com");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.sendCode("test@example.com"));
        assertEquals("发送验证码太频繁，请稍后再试", exception.getMessage());
    }

    @Test
    void verifyCode_shouldSuccess_whenCodeCorrect() {
        emailService.sendCode("test@example.com");
        String code = emailService.getCodeForTest("test@example.com");
        assertDoesNotThrow(() -> emailService.verifyCode("test@example.com", code));
    }

    @Test
    void verifyCode_shouldThrow_whenCodeNotSent() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.verifyCode("test@example.com", "123456"));
        assertEquals("请先发送验证码", exception.getMessage());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeWrong() {
        emailService.sendCode("test@example.com");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.verifyCode("test@example.com", "000000"));
        assertEquals("验证码错误", exception.getMessage());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeExpired() {
        emailService.sendCodeWithFixedCode("test@example.com", "123456", -6 * 60 * 1000);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.verifyCode("test@example.com", "123456"));
        assertEquals("验证码已过期，请重新获取", exception.getMessage());
    }

    @Test
    void verifyCode_shouldDeleteCodeAfterSuccess() {
        emailService.sendCode("test@example.com");
        String code = emailService.getCodeForTest("test@example.com");
        emailService.verifyCode("test@example.com", code);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.verifyCode("test@example.com", code));
        assertEquals("请先发送验证码", exception.getMessage());
    }
}
