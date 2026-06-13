package com.example.auth.service;

import com.example.auth.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class EmailService {

    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000;
    private static final long SEND_INTERVAL_MS = 60 * 1000;

    private final ConcurrentHashMap<String, VerificationCode> codeStore = new ConcurrentHashMap<>();

    public void sendCode(String email) {
        VerificationCode existing = codeStore.get(email);
        if (existing != null && !existing.isExpired()
                && (System.currentTimeMillis() - existing.getSentAt()) < SEND_INTERVAL_MS) {
            throw new BusinessException("发送验证码太频繁，请稍后再试");
        }

        String code = generateCode();
        VerificationCode vc = new VerificationCode(code, System.currentTimeMillis() + CODE_EXPIRE_MS, System.currentTimeMillis());
        codeStore.put(email, vc);

        // 模拟发送邮件，实际项目中这里调用邮件服务商 API
        log.info("【模拟邮件】邮箱：{}，验证码：{}", email, code);
    }

    /**
     * 测试专用：发送指定验证码，可设置过期时间偏移量
     */
    void sendCodeWithFixedCode(String email, String code, long expireOffsetMs) {
        VerificationCode vc = new VerificationCode(code, System.currentTimeMillis() + expireOffsetMs, System.currentTimeMillis());
        codeStore.put(email, vc);
    }

    /**
     * 测试专用：获取指定邮箱的验证码
     */
    String getCodeForTest(String email) {
        VerificationCode vc = codeStore.get(email);
        return vc != null ? vc.getCode() : null;
    }

    public void verifyCode(String email, String code) {
        VerificationCode vc = codeStore.get(email);
        if (vc == null) {
            throw new BusinessException("请先发送验证码");
        }
        if (vc.isExpired()) {
            codeStore.remove(email);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!vc.getCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证通过，删除验证码
        codeStore.remove(email);
    }

    private String generateCode() {
        int code = 100000 + ThreadLocalRandom.current().nextInt(900000);
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
