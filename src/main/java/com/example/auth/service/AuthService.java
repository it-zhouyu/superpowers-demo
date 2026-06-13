package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.User;
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
    private final SmsService smsService;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse loginOrRegister(LoginRequest request) {
        smsService.verifyCode(request.getPhone(), request.getCode());

        User user = findOrCreateUser(request.getPhone());

        String token = jwtService.generateToken(user.getId(), user.getPhone());

        log.info("用户登录成功：phone={}, userId={}", user.getPhone(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .build();
    }

    private User findOrCreateUser(String phone) {
        return userRepository.findByPhone(phone)
                .orElseGet(() -> createUser(phone));
    }

    private User createUser(String phone) {
        String nickname = "用户" + (100000 + ThreadLocalRandom.current().nextInt(900000));
        User user = User.builder()
                .phone(phone)
                .nickname(nickname)
                .build();
        try {
            user = userRepository.save(user);
            log.info("新用户注册：phone={}, userId={}, nickname={}", phone, user.getId(), nickname);
            return user;
        } catch (DataIntegrityViolationException e) {
            // 并发情况下，两个请求同时通过 findByPhone 检查，数据库唯一索引会拒绝重复插入
            // 捕获后重新查询即可
            log.warn("并发创建用户，重新查询：phone={}", phone);
            return userRepository.findByPhone(phone)
                    .orElseThrow(() -> e);
        }
    }
}
