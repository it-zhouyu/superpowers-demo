package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.BusinessException;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    public LoginResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public LoginResponse loginOrRegister(LoginRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());

        User user = findOrCreateUser(request.getEmail());

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        log.info("用户登录成功：email={}, userId={}", user.getEmail(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    private User findOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email));
    }

    private User createUser(String email) {
        String nickname = "用户" + (100000 + ThreadLocalRandom.current().nextInt(900000));
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .build();
        try {
            user = userRepository.save(user);
            log.info("新用户注册：email={}, userId={}, nickname={}", email, user.getId(), nickname);
            return user;
        } catch (DataIntegrityViolationException e) {
            // 并发情况下，两个请求同时通过 findByEmail 检查，数据库唯一索引会拒绝重复插入
            // 捕获后重新查询即可
            log.warn("并发创建用户，重新查询：email={}", email);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> e);
        }
    }
}
