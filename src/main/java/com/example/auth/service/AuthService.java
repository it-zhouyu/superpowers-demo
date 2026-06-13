package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final JwtService jwtService;

    private static final Random RANDOM = new Random();

    @Transactional
    public LoginResponse loginOrRegister(LoginRequest request) {
        smsService.verifyCode(request.getPhone(), request.getCode());

        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> createUser(request.getPhone()));

        String token = jwtService.generateToken(user.getId(), user.getPhone());

        log.info("用户登录成功：phone={}, userId={}", user.getPhone(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .build();
    }

    private User createUser(String phone) {
        String nickname = "用户" + (100000 + RANDOM.nextInt(900000));
        User user = User.builder()
                .phone(phone)
                .nickname(nickname)
                .build();
        user = userRepository.save(user);
        log.info("新用户注册：phone={}, userId={}, nickname={}", phone, user.getId(), nickname);
        return user;
    }
}
