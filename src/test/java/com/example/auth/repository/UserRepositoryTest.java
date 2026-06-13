package com.example.auth.repository;

import com.example.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByPhone_shouldReturnUser_whenPhoneExists() {
        User user = User.builder()
                .phone("13800138000")
                .nickname("测试用户")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByPhone("13800138000");

        assertTrue(found.isPresent());
        assertEquals("13800138000", found.get().getPhone());
        assertEquals("测试用户", found.get().getNickname());
    }

    @Test
    void findByPhone_shouldReturnEmpty_whenPhoneNotExists() {
        Optional<User> found = userRepository.findByPhone("13900139000");
        assertTrue(found.isEmpty());
    }

    @Test
    void save_shouldAutoGenerateId() {
        User user = User.builder()
                .phone("13800138000")
                .nickname("测试用户")
                .build();
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }
}
