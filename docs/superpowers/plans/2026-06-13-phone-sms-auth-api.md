# 手机号验证码登录注册 API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现基于手机号 + 短信验证码的登录注册 API，包含发送验证码、登录注册、JWT 认证三个核心功能

**Architecture:** 经典 Spring Boot 分层架构（Controller → Service → Repository），使用 Spring Security + JWT Filter 做无状态认证，H2 内存数据库存储用户数据，ConcurrentHashMap 存储验证码

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, H2, JJWT 0.12.x, Lombok, Maven

---

### Task 1: 初始化 Spring Boot 项目

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/auth/AuthApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>auth-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>auth-demo</name>
    <description>手机号验证码登录注册 Demo</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- H2 Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JJWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类 AuthApplication.java**

```java
package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建主配置 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:authdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: Y2xhdWRlLWNvZGUtYXV0aC1kZW1vLXNlY3JldC1rZXktMjAyNi0wNi0xMw==
  expiration: 7200000
```

- [ ] **Step 4: 创建测试配置 application.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false

jwt:
  secret: dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=
  expiration: 3600000
```

- [ ] **Step 5: Commit**

```bash
git init
git add .
git commit -m "feat: 初始化 Spring Boot 项目"
```

---

### Task 2: User 实体与 Repository（TDD）

**Files:**
- Create: `src/main/java/com/example/auth/entity/User.java`
- Create: `src/main/java/com/example/auth/repository/UserRepository.java`
- Create: `src/test/java/com/example/auth/repository/UserRepositoryTest.java`

- [ ] **Step 1: 写 UserRepository 的失败测试**

```java
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
        // 准备数据
        User user = User.builder()
                .phone("13800138000")
                .nickname("测试用户")
                .build();
        userRepository.save(user);

        // 执行查询
        Optional<User> found = userRepository.findByPhone("13800138000");

        // 验证
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
```

- [ ] **Step 2: 运行测试，确认编译失败**

Run: `cd /Users/dadudu/idea/vibe-coding-vip/superpowers-demo && mvn test -pl . -Dtest=UserRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，找不到 `User` 和 `UserRepository` 类

- [ ] **Step 3: 写 User 实体让测试编译通过**

```java
package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String phone;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

```java
package com.example.auth.repository;

import com.example.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);
}
```

- [ ] **Step 4: 运行测试，确认全部通过**

Run: `mvn test -Dtest=UserRepositoryTest`
Expected: 3 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: 添加 User 实体和 UserRepository（TDD）"
```

---

### Task 3: DTO 与异常体系

**Files:**
- Create: `src/main/java/com/example/auth/dto/SendCodeRequest.java`
- Create: `src/main/java/com/example/auth/dto/SendCodeResponse.java`
- Create: `src/main/java/com/example/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/example/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/example/auth/dto/ErrorResponse.java`
- Create: `src/main/java/com/example/auth/exception/BusinessException.java`

- [ ] **Step 1: 创建所有 DTO 类**

SendCodeRequest.java:
```java
package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

SendCodeResponse.java:
```java
package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResponse {

    private String message;
}
```

LoginRequest.java:
```java
package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    private String code;
}
```

LoginResponse.java:
```java
package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String phone;
    private String nickname;
}
```

ErrorResponse.java:
```java
package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int code;
    private String message;
    private LocalDateTime timestamp;
}
```

- [ ] **Step 2: 创建 BusinessException**

```java
package com.example.auth.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: 添加 DTO 类和 BusinessException"
```

---

### Task 4: SmsService 验证码服务（TDD）

**Files:**
- Create: `src/test/java/com/example/auth/service/SmsServiceTest.java`
- Create: `src/main/java/com/example/auth/service/SmsService.java`

- [ ] **Step 1: 写 SmsService 的失败测试**

```java
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
```

- [ ] **Step 2: 运行测试，确认编译失败**

Run: `mvn test -Dtest=SmsServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，找不到 `SmsService` 类

- [ ] **Step 3: 写 SmsService 实现让测试通过**

```java
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
```

- [ ] **Step 4: 运行测试，确认全部通过**

Run: `mvn test -Dtest=SmsServiceTest`
Expected: 7 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: 添加 SmsService 验证码发送与校验（TDD）"
```

---

### Task 5: JwtService（TDD）

**Files:**
- Create: `src/test/java/com/example/auth/service/JwtServiceTest.java`
- Create: `src/main/java/com/example/auth/service/JwtService.java`

- [ ] **Step 1: 写 JwtService 的失败测试**

```java
package com.example.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 使用测试密钥，与测试配置中的 secret 一致
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=";
        long expiration = 3600000; // 1 小时
        jwtService = new JwtService(testSecret, expiration);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken(1L, "13800138000");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseToken_shouldReturnCorrectClaims() {
        String token = jwtService.generateToken(1L, "13800138000");
        Claims claims = jwtService.parseToken(token);

        assertEquals("1", claims.getSubject());
        assertEquals("13800138000", claims.get("phone", String.class));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken(1L, "13800138000");
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void getPhoneFromToken_shouldReturnCorrectPhone() {
        String token = jwtService.generateToken(1L, "13800138000");
        String phone = jwtService.getPhoneFromToken(token);
        assertEquals("13800138000", phone);
    }

    @Test
    void validateToken_shouldReturnTrue_whenValid() {
        String token = jwtService.generateToken(1L, "13800138000");
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenInvalid() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }

    @Test
    void validateToken_shouldReturnFalse_whenEmpty() {
        assertFalse(jwtService.validateToken(""));
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

Run: `mvn test -Dtest=JwtServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，找不到 `JwtService` 类

- [ ] **Step 3: 写 JwtService 实现让测试通过**

```java
package com.example.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String phone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getPhoneFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("phone", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: 运行测试，确认全部通过**

Run: `mvn test -Dtest=JwtServiceTest`
Expected: 7 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: 添加 JwtService Token 生成与验证（TDD）"
```

---

### Task 6: AuthService 认证业务逻辑（TDD）

**Files:**
- Create: `src/test/java/com/example/auth/service/AuthServiceTest.java`
- Create: `src/main/java/com/example/auth/service/AuthService.java`

- [ ] **Step 1: 写 AuthService 的失败测试**

```java
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
    private SmsService smsService;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=";
        jwtService = new JwtService(testSecret, 3600000);
        authService = new AuthService(userRepository, smsService, jwtService);
    }

    @Test
    void loginOrRegister_shouldCreateNewUser_whenPhoneNotExists() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        LoginResponse response = authService.loginOrRegister(request);

        assertNotNull(response.getToken());
        assertEquals("13800138000", response.getPhone());
        assertTrue(response.getNickname().startsWith("用户"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginOrRegister_shouldReturnExistingUser_whenPhoneExists() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        User existingUser = User.builder()
                .id(1L)
                .phone("13800138000")
                .nickname("已有用户")
                .build();
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(existingUser));

        LoginResponse response = authService.loginOrRegister(request);

        assertNotNull(response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("13800138000", response.getPhone());
        assertEquals("已有用户", response.getNickname());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginOrRegister_shouldCallSmsVerify() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        authService.loginOrRegister(request);
        verify(smsService).verifyCode("13800138000", "123456");
    }

    @Test
    void loginOrRegister_shouldThrow_whenSmsVerifyFails() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("000000");

        doThrow(new BusinessException("验证码错误"))
                .when(smsService).verifyCode(anyString(), anyString());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.loginOrRegister(request));
        assertEquals("验证码错误", exception.getMessage());
        verify(userRepository, never()).findByPhone(anyString());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败**

Run: `mvn test -Dtest=AuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，找不到 `AuthService` 类

- [ ] **Step 3: 写 AuthService 实现让测试通过**

```java
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
        // 校验验证码
        smsService.verifyCode(request.getPhone(), request.getCode());

        // 查找或创建用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> createUser(request.getPhone()));

        // 生成 Token
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
```

- [ ] **Step 4: 运行测试，确认全部通过**

Run: `mvn test -Dtest=AuthServiceTest`
Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: 添加 AuthService 登录注册业务逻辑（TDD）"
```

---

### Task 7: JWT 认证过滤器

**Files:**
- Create: `src/main/java/com/example/auth/filter/JwtAuthenticationFilter.java`

- [ ] **Step 1: 创建 JwtAuthenticationFilter**

```java
package com.example.auth.filter;

import com.example.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtService.validateToken(token)) {
            Claims claims = jwtService.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String phone = claims.get("phone", String.class);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            authentication.setDetails(phone);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: 添加 JwtAuthenticationFilter 认证过滤器"
```

---

### Task 8: Spring Security 配置

**Files:**
- Create: `src/main/java/com/example/auth/config/SecurityConfig.java`

- [ ] **Step 1: 创建 SecurityConfig**

```java
package com.example.auth.config;

import com.example.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（无状态 REST API 不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 CORS（演示阶段）
                .cors(AbstractHttpConfigurer::disable)
                // 无状态 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置路径权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/send-code", "/api/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                // H2 控制台需要允许 frame
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                // 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: 添加 Spring Security 配置"
```

---

### Task 9: 全局异常处理与 AuthController（TDD）

**Files:**
- Create: `src/main/java/com/example/auth/exception/GlobalExceptionHandler.java`
- Create: `src/test/java/com/example/auth/controller/AuthControllerTest.java`
- Create: `src/main/java/com/example/auth/controller/AuthController.java`

- [ ] **Step 1: 创建 GlobalExceptionHandler**

```java
package com.example.auth.exception;

import com.example.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(e.getCode()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        ErrorResponse error = ErrorResponse.builder()
                .code(400)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(401)
                .message("认证失败：" + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(403)
                .message("没有访问权限")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(500)
                .message("服务器内部错误")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

- [ ] **Step 2: 写 AuthController 的失败测试**

```java
package com.example.auth.controller;

import com.example.auth.dto.SendCodeRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.service.AuthService;
import com.example.auth.service.SmsService;
import com.example.auth.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
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
    private SmsService smsService;

    @MockBean
    private AuthService authService;

    @Test
    void sendCode_shouldReturn200_whenPhoneValid() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone("13800138000");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码发送成功"));

        verify(smsService).sendCode("13800138000");
    }

    @Test
    void sendCode_shouldReturn400_whenPhoneInvalid() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone("123");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));
    }

    @Test
    void sendCode_shouldReturn400_whenPhoneEmpty() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone("");

        mockMvc.perform(post("/api/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号不能为空"));
    }

    @Test
    void login_shouldReturn200_whenSuccess() throws Exception {
        LoginResponse mockResponse = LoginResponse.builder()
                .token("test-token")
                .userId(1L)
                .phone("13800138000")
                .nickname("用户123456")
                .build();
        when(authService.loginOrRegister(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }

    @Test
    void login_shouldReturn400_whenCodeInvalid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
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
```

- [ ] **Step 3: 运行测试，确认编译失败**

Run: `mvn test -Dtest=AuthControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，找不到 `AuthController` 类

- [ ] **Step 4: 写 AuthController 实现让测试通过**

```java
package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.entity.User;
import com.example.auth.exception.BusinessException;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.AuthService;
import com.example.auth.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final SmsService smsService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/send-code")
    public ResponseEntity<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        smsService.sendCode(request.getPhone());
        SendCodeResponse response = SendCodeResponse.builder()
                .message("验证码发送成功")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.loginOrRegister(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String phone = (String) authentication.getDetails();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LoginResponse response = LoginResponse.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .build();
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 5: 运行测试，确认全部通过**

Run: `mvn test -Dtest=AuthControllerTest`
Expected: 6 tests PASSED

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: 添加 AuthController 认证接口和全局异常处理（TDD）"
```

---

### Task 10: 全量测试与集成验证

**Files:**
- No new files

- [ ] **Step 1: 运行全部单元测试**

Run: `mvn test`
Expected: 所有测试通过（UserRepositoryTest 3 + SmsServiceTest 7 + JwtServiceTest 7 + AuthServiceTest 4 + AuthControllerTest 6 = 27 tests PASSED）

- [ ] **Step 2: 启动应用**

Run: `mvn spring-boot:run`
Expected: `Started AuthApplication in X.XX seconds`

- [ ] **Step 3: 测试发送验证码**

Run: `curl -X POST http://localhost:8080/api/send-code -H "Content-Type: application/json" -d '{"phone":"13800138000"}'`
Expected: `{"message":"验证码发送成功"}`

同时在应用控制台日志中看到 `【模拟短信】手机号：13800138000，验证码：XXXXXX`

- [ ] **Step 4: 测试登录（用控制台日志中的验证码替换 XXXXXX）**

Run: `curl -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '{"phone":"13800138000","code":"XXXXXX"}'`
Expected: 返回包含 token、userId、phone、nickname 的 JSON

- [ ] **Step 5: 测试获取当前用户信息（用上一步返回的 token 替换 TOKEN）**

Run: `curl http://localhost:8080/api/me -H "Authorization: Bearer TOKEN"`
Expected: 返回包含 userId、phone、nickname 的 JSON

- [ ] **Step 6: 测试异常情况 — 手机号格式错误**

Run: `curl -X POST http://localhost:8080/api/send-code -H "Content-Type: application/json" -d '{"phone":"123"}'`
Expected: `{"code":400,"message":"手机号格式不正确","timestamp":"..."}`

- [ ] **Step 7: 测试异常情况 — 验证码错误**

先发送验证码，然后用错误验证码登录：
Run: `curl -X POST http://localhost:8080/api/send-code -H "Content-Type: application/json" -d '{"phone":"13800138001"}'`
Run: `curl -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '{"phone":"13800138001","code":"000000"}'`
Expected: `{"code":400,"message":"验证码错误","timestamp":"..."}`

- [ ] **Step 8: 测试异常情况 — 无 Token 访问受保护接口**

Run: `curl http://localhost:8080/api/me`
Expected: 返回 401 状态码

- [ ] **Step 9: 最终 Commit**

```bash
git add .
git commit -m "feat: 手机号验证码登录注册 API 完成，全部测试通过"
```
