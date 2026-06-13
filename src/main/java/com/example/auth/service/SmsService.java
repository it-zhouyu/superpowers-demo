package com.example.auth.service;

import com.example.auth.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SmsService {

    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000;
    private static final long SEND_INTERVAL_MS = 60 * 1000;

    private final ConcurrentHashMap<String, VerificationCode> codeStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void sendCode(String phone) {
        VerificationCode existing = codeStore.get(phone);
        if (existing != null && !existing.isExpired()
                && (System.currentTimeMillis() - existing.getSentAt()) < SEND_INTERVAL_MS) {
            throw new BusinessException("发送验证码太频繁，请稍后再试");
        }

        String code = generateCode();
        VerificationCode vc = new VerificationCode(code, System.currentTimeMillis() + CODE_EXPIRE_MS, System.currentTimeMillis());
        codeStore.put(phone, vc);

        // 模拟发送短信，实际项目中这里调用短信服务商 API
        log.info("【模拟短信】手机号：{}，验证码：{}", phone, code);
    }

    /**
     * 测试专用：发送指定验证码，可设置过期时间偏移量
     */
    void sendCodeWithFixedCode(String phone, String code, long expireOffsetMs) {
        VerificationCode vc = new VerificationCode(code, System.currentTimeMillis() + expireOffsetMs, System.currentTimeMillis());
        codeStore.put(phone, vc);
    }

    /**
     * 测试专用：获取指定手机号的验证码
     */
    String getCodeForTest(String phone) {
        VerificationCode vc = codeStore.get(phone);
        return vc != null ? vc.getCode() : null;
    }

    public void verifyCode(String phone, String code) {
        VerificationCode vc = codeStore.get(phone);
        if (vc == null) {
            throw new BusinessException("请先发送验证码");
        }
        if (vc.isExpired()) {
            codeStore.remove(phone);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!vc.getCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证通过，删除验证码
        codeStore.remove(phone);
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private static class VerificationCode {
        private final String code;
        private final long expireAt;
        private final long sentAt;

        VerificationCode(String code, long expireAt, long sentAt) {
            this.code = code;
            this.expireAt = expireAt;
            this.sentAt = sentAt;
        }

        String getCode() { return code; }
        long getSentAt() { return sentAt; }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
