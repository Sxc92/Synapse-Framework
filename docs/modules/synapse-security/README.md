# Synapse Security 模块

## 概述

Synapse Security 模块是 Synapse Framework 的安全认证和授权模块，提供了完整的身份认证、权限控制和安全管理功能。使用自研的 TokenService 和 PermissionService，支持多种认证方式和细粒度的权限控制。

## 主要特性

- 🔐 **多种认证方式**：用户名密码、OAuth2.0（通过 AuthRequest 传递）、Token验证等
- 🛡️ **权限控制**：基于注解的权限验证（@RequireLogin、@RequireRole、@RequirePermission）
- 🔑 **角色管理**：支持多角色和角色继承
- 🚪 **登录管理**：Token生成、验证、续期、撤销
- 📱 **多端支持**：Web、移动端、小程序等
- 🔒 **安全防护**：XSS、CSRF、SQL注入防护
- 📊 **操作审计**：完整的操作日志记录
- 🔗 **双通道认证**：区分外部请求（Gateway）和内部调用（服务间），支持 Feign 自动签名
- 🔄 **滑动过期**：Token 自动续期，支持菜单和资源的同步续期

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-security</artifactId>
    <version>${synapse.version}</version>
</dependency>
```

### 2. 基础配置

```yaml
# Synapse Security 配置
synapse:
  security:
    enabled: true                    # 是否启用安全模块
    mode: STRICT                      # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    security-logging: true           # 是否启用安全日志
    security-log-level: INFO         # 安全日志级别
    white-list:                      # 白名单路径配置
      enabled: true
      paths:
        - /api/auth/login
        - /api/auth/logout
        - /actuator/**
    token:
      timeout: 7200                  # Token 过期时间（秒），默认 2 小时
      enable-sliding-expiration: true # 启用滑动过期（自动刷新）
      refresh-threshold: 600          # 刷新阈值（秒），当 token 剩余时间少于 10 分钟时自动续期
      renewal-duration: 7200         # 续期时长（秒），刷新时将过期时间延长到 2 小时
```

### 3. 基础使用示例

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        // 查询用户信息
        User user = userService.findByUsername(request.getUsername());
        List<String> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 构建认证请求（包含用户完整信息）
        AuthRequest authRequest = AuthRequest.builder()
            .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
            .usernamePasswordAuth(UsernamePasswordAuth.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build())
            .userId(user.getId().toString())
            .realName(user.getRealName())
            .email(user.getEmail())
            .mobile(user.getMobile())
            .avatar(user.getAvatar())
            .roles(roles)
            .permissions(permissions)
            .build();
        
        // 调用认证服务
        AuthResponse response = authenticationService.authenticate(authRequest);
        return Result.success(response);
    }
    
    @RequireLogin
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        UserContext currentUser = UserContext.getCurrentUser();
        return Result.success(userService.getProfile(currentUser.getUserId()));
    }
    
    @RequireRole("admin")
    @GetMapping("/admin")
    public Result<String> adminOnly() {
        return Result.success("管理员专用接口");
    }
    
    @RequirePermission("user:read")
    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        return Result.success(userService.list());
    }
}
```

## 核心组件

### 1. AuthenticationService
统一的认证服务接口，支持多种认证方式：
- 用户名密码认证
- OAuth2.0认证
- Token验证
- Token续期

### 2. TokenService
Token管理服务，提供：
- UUID Token生成
- Token验证
- Token续期（支持滑动过期）
- Token撤销
- 用户会话管理

### 3. PermissionService
权限检查服务，支持：
- 登录检查
- 角色权限检查
- 权限验证
- 支持 AND/OR 逻辑

### 4. PermissionAspect
权限检查切面（AOP），自动拦截：
- @RequireLogin 注解
- @RequireRole 注解
- @RequirePermission 注解

**注意**：当 `synapse.security.mode` 设置为 `DISABLED` 时，权限检查会被跳过。

### 5. UserContextInterceptor
用户上下文拦截器，负责：
- 从请求中提取 Token
- 设置用户上下文到 ThreadLocal
- Token 自动续期（滑动过期）

## Token 滑动过期机制

### 工作原理

1. **自动检测**：每次请求时，`UserContextInterceptor` 会检查 Token 剩余时间
2. **自动续期**：如果剩余时间少于 `refresh-threshold`（默认 10 分钟），自动续期
3. **同步续期**：续期时会同时续期以下数据：
   - Session（会话）
   - Token
   - Permissions（权限）
   - Roles（角色）
   - Menus（菜单）
   - Resources（资源）
   - Systems（系统）

### 配置说明

```yaml
synapse:
  security:
    token:
      # Token 过期时间（秒）
      timeout: 7200
      # 是否启用滑动过期（自动刷新）
      enable-sliding-expiration: true
      # 刷新阈值（秒），当 token 剩余时间少于此值时自动刷新 token
      refresh-threshold: 600  # 10 分钟
      # 续期时长（秒），刷新 token 时将过期时间延长到此值
      renewal-duration: 7200  # 2 小时
```

## 权限注解使用

### @RequireLogin
要求用户必须登录才能访问：

```java
@RequireLogin
@GetMapping("/profile")
public Result<UserProfile> getProfile() {
    UserContext currentUser = UserContext.getCurrentUser();
    return Result.success(userService.getProfile(currentUser.getUserId()));
}
```

### @RequireRole
要求用户必须具有指定角色：

```java
@RequireRole("admin")
@GetMapping("/admin")
public Result<String> adminOnly() {
    return Result.success("管理员专用接口");
}

// 支持多个角色（OR 逻辑）
@RequireRole({"admin", "super_admin"})
@GetMapping("/super-admin")
public Result<String> superAdminOnly() {
    return Result.success("超级管理员专用接口");
}
```

### @RequirePermission
要求用户必须具有指定权限：

```java
@RequirePermission("user:read")
@GetMapping("/users")
public Result<List<User>> getUsers() {
    return Result.success(userService.list());
}

// 支持多个权限（AND 逻辑）
@RequirePermission({"user:read", "user:write"})
@PostMapping("/users")
public Result<User> createUser(@RequestBody User user) {
    return Result.success(userService.createUser(user));
}
```

## 用户上下文使用

### 获取当前用户信息

```java
@Service
public class UserService {
    
    public void updateProfile(UpdateProfileDTO dto) {
        // 从上下文获取当前用户信息
        String userId = UserContext.getCurrentUserId();
        String account = UserContext.getCurrentAccount();
        String realName = UserContext.getCurrentRealName();
        String email = UserContext.getCurrentEmail();
        String mobile = UserContext.getCurrentMobile();
        String avatar = UserContext.getCurrentAvatar();
        
        // 获取角色和权限
        List<String> roles = UserContext.getCurrentRoles();
        List<String> permissions = UserContext.getCurrentPermissions();
        
        // 权限和角色检查
        if (UserContext.hasRole("admin")) {
            // 管理员逻辑
        }
        
        if (UserContext.hasPermission("user:read")) {
            // 有读取权限
        }
        
        // 或者获取完整用户上下文
        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            Ex.throwEx(StandardErrorCode.USER_NOT_LOGIN);
        }
    }
}
```

## 异常处理

### 使用 Ex.throwEx() 统一异常处理

```java
@Service
public class UserService {
    
    public UserVO getUser(String id) {
        if (id == null || id.isEmpty()) {
            Ex.throwEx(StandardErrorCode.USER_ID_REQUIRED, "用户ID不能为空");
        }
        
        User user = userRepository.getById(id);
        if (user == null) {
            Ex.throwEx(StandardErrorCode.USER_NOT_FOUND, id);
        }
        
        return VoMapper.toVO(user, UserVO.class);
    }
}
```

## Gateway 签名验证

### 配置

```yaml
synapse:
  security:
    gateway-signature:
      enabled: true
      secret: "your-gateway-secret-key"
      validity-window: 300000  # 5 分钟
      enable-context-passing: true
```

### 工作原理

1. Gateway 在请求头中添加签名
2. 服务端验证签名有效性
3. 验证通过后，从请求头中提取用户上下文信息
4. 设置到 ThreadLocal 中供业务代码使用

## 内部服务调用签名

### 配置

```yaml
synapse:
  security:
    internal-service:
      enabled: true
      service-name: "your-service-name"
      secret: "your-service-secret"
      validity-window: 300000  # 5 分钟
      allowed-services:
        service1: "secret1"
        service2: "secret2"
```

### Feign 自动签名

框架提供了 Feign 拦截器，自动为 Feign 请求添加签名：

```java
@FeignClient(name = "other-service")
public interface OtherServiceClient {
    
    @GetMapping("/api/data")
    Result<DataVO> getData();
}
```

## 最佳实践

### 1. Token 管理
- 设置合理的 Token 过期时间
- 启用滑动过期，提升用户体验
- 定期清理过期的 Token

### 2. 权限设计
- 使用 RBAC 模型设计权限
- 权限码命名规范：`模块:操作`（如 `user:create`）
- 角色继承关系清晰

### 3. 安全防护
- 启用 Gateway 签名验证
- 使用 HTTPS 传输
- 定期更新密钥

### 4. 异常处理
- 使用 `Ex.throwEx()` 统一异常处理
- 错误消息支持国际化
- 记录完整的操作日志

## 故障排除

### 常见问题

1. **Token 过期**
   - 检查 Token 有效期配置
   - 确认滑动过期已启用
   - 检查 `refresh-threshold` 配置

2. **权限验证失败**
   - 检查权限码配置
   - 验证用户角色分配
   - 确认 `mode` 不是 `DISABLED`

3. **菜单和资源未续期**
   - 确认 `renewToken` 方法中调用了 `extendUserMenusAndResources`
   - 检查缓存配置是否正确

4. **用户信息字段为 null**
   - 确认登录时 `AuthRequest` 中设置了所有字段（realName、email、mobile、avatar）
   - 检查 `storeUserSession` 方法是否正确构建了 `UserContext`

### 日志配置

```yaml
logging:
  level:
    com.indigo.security: DEBUG
    com.indigo.cache: DEBUG
```

## 版本历史

| 版本 | 更新内容 |
|------|----------|
| 1.0.0 | 初始版本，基础认证功能 |
| 1.1.0 | 添加权限控制功能 |
| 1.2.0 | 集成自研 TokenService |
| 1.3.0 | 添加滑动过期、自动续期功能 |
| 1.4.0 | 添加 Gateway 签名验证 |
| 1.5.0 | 优化性能和稳定性，支持菜单和资源自动续期 |

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。
