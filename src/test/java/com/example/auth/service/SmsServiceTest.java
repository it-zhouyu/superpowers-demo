package com.example.auth.service;

import com.example.auth.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsServiceTest {

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
    }

    @Test
    void sendCode_shouldSuccess_whenFirstTime() {
        assertDoesNotThrow(() -> smsService.sendCode("13800138000"));
    }

    @Test
    void sendCode_shouldThrow_whenSendTooFrequently() {
        smsService.sendCode("13800138000");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsService.sendCode("13800138000"));
        assertEquals("发送验证码太频繁，请稍后再试", exception.getMessage());
    }

    @Test
    void verifyCode_shouldSuccess_whenCodeCorrect() {
        smsService.sendCode("13800138000");
        String code = smsService.getCodeForTest("13800138000");
        assertDoesNotThrow(() -> smsService.verifyCode("13800138000", code));
    }

    @Test
    void verifyCode_shouldThrow_whenCodeNotSent() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsService.verifyCode("13800138000", "123456"));
        assertEquals("请先发送验证码", exception.getMessage());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeWrong() {
        smsService.sendCode("13800138000");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsService.verifyCode("13800138000", "000000"));
        assertEquals("验证码错误", exception.getMessage());
    }

    @Test
    void verifyCode_shouldThrow_whenCodeExpired() {
        smsService.sendCodeWithFixedCode("13800138000", "123456", -6 * 60 * 1000);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsService.verifyCode("13800138000", "123456"));
        assertEquals("验证码已过期，请重新获取", exception.getMessage());
    }

    @Test
    void verifyCode_shouldDeleteCodeAfterSuccess() {
        smsService.sendCode("13800138000");
        String code = smsService.getCodeForTest("13800138000");
        smsService.verifyCode("13800138000", code);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsService.verifyCode("13800138000", code));
        assertEquals("请先发送验证码", exception.getMessage());
    }
}
