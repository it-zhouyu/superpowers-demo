## 手机号验证码登录注册 API 设计文档

### 技术栈

- Java 17 + Spring Boot 3.x
- Spring Web（REST API）
- Spring Data JPA（数据访问层）
- Spring Security（安全框架）
- H2 内存数据库（开发阶段，无需安装）
- JJWT（JWT Token 生成与验证）
- Lombok（减少样板代码）
- Maven（构建工具）

### 项目结构

```
src/main/java/com/example/auth/
├── AuthApplication.java              // 启动类
├── controller/
│   └── AuthController.java           // 认证相关接口
├── service/
│   ├── SmsService.java               // 短信验证码发送服务（模拟）
│   ├── AuthService.java              // 认证业务逻辑
│   └── JwtService.java               // JWT Token 生成与验证
├── repository/
│   └── UserRepository.java           // 用户数据访问
├── entity/
│   └── User.java                     // 用户实体
├── dto/
│   ├── SendCodeRequest.java          // 发送验证码请求体
│   ├── SendCodeResponse.java         // 发送验证码响应体
│   ├── LoginRequest.java             // 登录请求体
│   └── LoginResponse.java            // 登录响应体
├── config/
│   └── SecurityConfig.java           // 安全配置（放行认证接口）
├── filter/
│   └── JwtAuthenticationFilter.java  // JWT 认证过滤器
└── exception/
    ├── GlobalExceptionHandler.java   // 全局异常处理
    └── BusinessException.java        // 业务异常
```

### API 接口

- POST `/api/send-code` — 发送验证码，不需要 Token
- POST `/api/login` — 验证码登录/注册，不需要 Token
- GET `/api/me` — 获取当前用户信息，需要 Token

### 核心业务流程

#### 发送验证码

1. 客户端发送 `POST /api/send-code`，请求体包含手机号
2. 后端校验手机号格式（11 位数字）
3. 检查该手机号是否在 60 秒内已经发送过验证码，如果是则拒绝
4. 生成 6 位随机数字验证码
5. 将验证码存储到内存（ConcurrentHashMap，key 为手机号，value 为验证码 + 过期时间 + 发送时间）
6. 模拟发送短信（打印到控制台日志）
7. 返回成功响应

#### 登录/注册

1. 客户端发送 `POST /api/login`，请求体包含手机号和验证码
2. 后端校验手机号格式和验证码格式（6 位数字）
3. 从内存中取出该手机号对应的验证码，校验是否匹配、是否过期（5 分钟有效期）
4. 验证通过后，删除已使用的验证码
5. 查询数据库，判断该手机号是否已注册
   - 已注册：直接获取用户信息
   - 未注册：自动创建新用户（手机号即为账号，无需额外注册步骤）
6. 生成 JWT Access Token（有效期 2 小时）
7. 返回 Token 和用户信息

#### JWT 认证

1. 用户登录成功后获得 JWT Token
2. 后续请求在 HTTP Header 中携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` 拦截请求，从 Header 中提取 Token
4. 验证 Token 的签名和有效期
5. 从 Token 中解析出用户信息，设置到 Spring Security 上下文中
6. Token 过期或无效则返回 401 Unauthorized

### 验证码存储策略

使用 `ConcurrentHashMap<String, VerificationCode>` 在内存中存储验证码信息：

```
VerificationCode {
    code: "123456"           // 6 位验证码
    expireAt: 1718300000000  // 过期时间戳（5 分钟后）
    sentAt: 1718299700000    // 发送时间戳（用于 60 秒频率限制）
}
```

不使用 Redis，因为当前是演示项目，内存存储足够，学员也更容易理解

### 数据模型

#### User 实体

```
User {
    id: Long           // 主键，自增
    phone: String      // 手机号，唯一索引，11 位
    nickname: String   // 昵称，注册时自动生成（如"用户123456"）
    createdAt: Date    // 创建时间
    updatedAt: Date    // 更新时间
}
```

#### 数据库配置

- 使用 H2 内存数据库，应用启动时自动创建表，关闭后数据丢失
- 通过 `application.yml` 配置 JPA 自动建表（`ddl-auto: update`）
- H2 自带控制台，可通过 `/h2-console` 访问，方便调试查看数据

### 安全配置

#### JWT 配置

- 密钥：在 `application.yml` 中配置一个 Base64 编码的密钥字符串
- Access Token 有效期：2 小时
- Token 中携带的信息：用户 ID（subject）、手机号（claim）、签发时间、过期时间

#### Spring Security 配置

- `/api/send-code`、`/api/login` 放行，不需要认证
- `/h2-console/**` 放行，方便调试
- `/api/me` 及其他接口需要认证
- 禁用 CSRF（因为是无状态的 REST API，不使用 Session）
- 禁用 CORS（演示阶段，后续可按需开启）

### 全局异常处理

统一错误响应格式：

```json
{
    "code": 400,
    "message": "手机号格式不正确",
    "timestamp": "2026-06-13T10:00:00"
}
```

覆盖的异常类型：
- 业务异常（参数校验失败、验证码错误、验证码过期等）→ 返回 400
- Token 无效或过期 → 返回 401
- 其他未预期异常 → 返回 500
