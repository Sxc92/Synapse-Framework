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
    mode: STRICT                     # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    security-logging: true           # 是否启用安全日志
    security-log-level: INFO         # 安全日志级别
    white-list:                      # 白名单路径配置
      enabled: true
      paths:
        - /api/auth/login
        - /api/auth/logout
        - /actuator/**
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
        
        // 构建认证请求
        AuthRequest authRequest = AuthRequest.builder()
            .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
            .usernamePasswordAuth(UsernamePasswordAuth.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build())
            .userId(user.getId().toString())
            .roles(roles)
            .permissions(permissions)
            .build();
        
        // 调用认证服务
        return authenticationService.authenticate(authRequest);
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
- Token续期
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

### 5. TokenManager（已废弃）
基于 TokenService 的Token管理服务（保留用于向后兼容）

**注意：** `PermissionManager` 已删除，请使用 `PermissionService` 替代。

### 6. DataPermissionService
数据权限服务，支持：
- 数据范围控制
- 自定义权限规则
- 多维度权限控制

## 认证服务使用

### 1. 基础认证

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final AuthenticationService authenticationService;
    
    public Result<AuthResponse> login(String username, String password) {
        // 查询用户信息
        User user = userRepository.findByUsername(username);
        if (user == null || !PasswordUtils.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        
        // 获取用户角色和权限
        List<String> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 构建认证请求
        AuthRequest request = AuthRequest.builder()
            .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
            .usernamePasswordAuth(UsernamePasswordAuth.builder()
                .username(username)
                .password(password)
                .build())
            .userId(user.getId().toString())
            .roles(roles)
            .permissions(permissions)
            .build();
        
        // 调用认证服务
        return authenticationService.authenticate(request);
    }
}
```

### 2. OAuth2.0认证

```java
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    
    private final AuthenticationService authenticationService;
    
    public Result<AuthResponse> oauth2Login(String code, String state) {
        // OAuth2.0授权码验证逻辑
        OAuth2UserInfo oauth2User = validateOAuth2Code(code, state);
        
        // 获取或创建本地用户
        User user = getOrCreateUser(oauth2User);
        
        // 获取用户角色和权限
        List<String> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 构建OAuth2认证请求
        AuthRequest request = AuthRequest.builder()
            .authType(AuthRequest.AuthType.OAUTH2_AUTHORIZATION_CODE)
            .oauth2Auth(OAuth2Auth.builder()
                .clientId("oauth2_client_id")
                .clientSecret("oauth2_client_secret")
                .code(code)
                .redirectUri("http://localhost:8080/callback")
                .provider("github")
                .build())
            .userId(user.getId().toString())
            .roles(roles)
            .permissions(permissions)
            .build();
        
        // 调用认证服务
        return authenticationService.authenticate(request);
    }
}
```

### 3. Token续期

```java
@Service
@RequiredArgsConstructor
public class TokenRenewalService {
    
    private final AuthenticationService authenticationService;
    
    public Result<AuthResponse> renewToken(String token) {
        return authenticationService.renewToken(token);
    }
}
```

### 4. 用户登出

```java
@Service
@RequiredArgsConstructor
public class LogoutService {
    
    private final TokenService tokenService;
    
    public Result<Void> logout(String token) {
        if (token != null) {
            tokenService.revokeToken(token);
        }
        return Result.success();
    }
}
```

### 5. 获取当前用户

```java
@Service
public class UserService {
    
    public UserProfile getCurrentUserProfile() {
        // 从 UserContext 获取当前用户信息
        UserContext currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotLoginException("用户未登录");
        }
        
        return userRepository.getProfile(currentUser.getUserId());
    }
}
```

## 权限控制使用

### 1. 注解权限检查

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    // 要求登录
    @RequireLogin
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        UserContext currentUser = UserContext.getCurrentUser();
        return Result.success(userService.getProfile(currentUser.getUserId()));
    }
    
    // 要求特定权限
    @RequirePermission("user:read")
    @GetMapping("/{userId}")
    public Result<UserInfo> getUserInfo(@PathVariable String userId) {
        return Result.success(userService.getUserInfo(userId));
    }
    
    // 要求多个权限（AND逻辑：需要所有权限）
    @RequirePermission(value = {"user:read", "user:write"}, logical = Logical.AND)
    @PutMapping("/{userId}")
    public Result<UserInfo> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUser(userId, request));
    }
    
    // 要求多个权限（OR逻辑：需要任一权限）
    @RequirePermission(value = {"user:read", "user:view"}, logical = Logical.OR)
    @GetMapping("/list")
    public Result<List<UserInfo>> getUserList() {
        return Result.success(userService.getUserList());
    }
    
    // 要求特定角色
    @RequireRole("admin")
    @PostMapping("/create")
    public Result<UserInfo> createUser(@RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }
    
    // 要求多个角色（OR逻辑：需要任一角色）
    @RequireRole(value = {"admin", "super_admin"}, logical = Logical.OR)
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return Result.success();
    }
    
    // 组合使用多个注解
    @RequireLogin
    @RequireRole("admin")
    @RequirePermission("user:manage")
    @GetMapping("/admin/users")
    public Result<List<UserInfo>> getAdminUsers() {
        return Result.success(userService.getAdminUsers());
    }
}
```

### 2. 编程式权限检查

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final PermissionService permissionService;
    
    public void updateUser(String userId, UpdateUserRequest request) {
        // 检查登录
        permissionService.checkLogin();
        
        // 检查权限
        permissionService.checkPermission("user:update");
        
        // 检查角色
        permissionService.checkRole("admin");
        
        // 检查多个权限（AND逻辑）
        permissionService.checkPermission(
            new String[]{"user:read", "user:write"}, 
            Logical.AND
        );
        
        // 检查多个角色（OR逻辑）
        permissionService.checkRole(
            new String[]{"admin", "manager"}, 
            Logical.OR
        );
        
        // 执行更新逻辑
        userRepository.updateUser(userId, request);
    }
}
```

### 3. 从请求中获取Token

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final TokenService tokenService;
    
    @RequireLogin
    @GetMapping("/token-info")
    public Result<TokenInfo> getTokenInfo(HttpServletRequest request) {
        // 从请求中获取 token
        String token = getTokenFromRequest(request);
        
        // 验证 token
        boolean isValid = tokenService.validateToken(token);
        
        // 获取用户上下文
        UserContext userContext = tokenService.getUserContext(token);
        
        // 获取剩余时间
        long remainingTime = tokenService.getTokenRemainingTime(token);
        
        return Result.success(new TokenInfo(isValid, remainingTime, userContext));
    }
    
    /**
     * 从请求中获取 token
     * 优先级：
     * 1. 从请求属性中获取（UserContextInterceptor 设置的）
     * 2. 从请求头中获取（Authorization Bearer 或 X-Auth-Token）
     * 3. 从查询参数中获取（token）
     * 
     * 注意：
     * - 需要导入 com.indigo.security.constants.SecurityConstants
     * - 需要注入 SecurityProperties 并使用 TokenConfigHelper 获取配置值
     */
    private String getTokenFromRequest(HttpServletRequest request, SecurityProperties securityProperties) {
        // 获取配置值
        String tokenHeaderName = TokenConfigHelper.getTokenHeaderName(securityProperties);
        String tokenPrefix = TokenConfigHelper.getTokenPrefix(securityProperties);
        int prefixLength = TokenConfigHelper.getTokenPrefixLength(securityProperties);
        String xAuthTokenHeader = TokenConfigHelper.getXAuthTokenHeader(securityProperties);
        String tokenQueryParam = TokenConfigHelper.getTokenQueryParam(securityProperties);
        
        // 1. 优先从请求属性中获取
        Object tokenObj = request.getAttribute(SecurityConstants.REQUEST_ATTR_TOKEN);
        if (tokenObj instanceof String token && token != null && !token.trim().isEmpty()) {
            return token;
        }
        
        // 2. 从请求头中获取
        String authHeader = request.getHeader(tokenHeaderName);
        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(prefixLength);
        }
        
        String tokenHeader = request.getHeader(xAuthTokenHeader);
        if (tokenHeader != null && !tokenHeader.trim().isEmpty()) {
            return tokenHeader;
        }
        
        // 3. 从查询参数中获取
        String tokenParam = request.getParameter(tokenQueryParam);
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam;
        }
        
        return null;
    }
}
```

## 数据权限使用

### 1. 数据权限规则配置

```java
@Service
@RequiredArgsConstructor
public class UserDataService {
    
    private final DataPermissionService dataPermissionService;
    
    public List<User> getUsersWithPermission() {
        UserContext currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotLoginException("用户未登录");
        }
        
        // 获取数据范围
        String dataScope = dataPermissionService.getDataScope(currentUser, "user");
        
        // 根据数据范围查询用户
        return userRepository.findByDataScope(dataScope);
    }
}
```

### 2. 自定义数据权限

```java
@Component
public class CustomDataPermissionHandler {
    
    public String buildDataScope(UserContext user, String resourceType) {
        switch (resourceType) {
            case "user":
                return buildUserDataScope(user);
            case "order":
                return buildOrderDataScope(user);
            default:
                return "1=1"; // 默认无限制
        }
    }
    
    private String buildUserDataScope(UserContext user) {
        if (user.hasRole("SUPER_ADMIN")) {
            return "1=1"; // 超级管理员可以查看所有用户
        }
        
        if (user.hasRole("ADMIN")) {
            return "dept_id = " + user.getDeptId(); // 管理员只能查看本部门用户
        }
        
        return "create_user_id = " + user.getUserId(); // 普通用户只能查看自己创建的用户
    }
    
    private String buildOrderDataScope(UserContext user) {
        if (user.hasRole("SUPER_ADMIN")) {
            return "1=1";
        }
        
        if (user.hasRole("ADMIN")) {
            return "dept_id = " + user.getDeptId();
        }
        
        return "user_id = " + user.getUserId(); // 普通用户只能查看自己的订单
    }
}
```

## 认证流程

### 外部请求流程（Gateway）

```
客户端请求 → Gateway验证Token → 注入用户上下文 → 转发到微服务 → 微服务验证签名 → 设置UserContext
```

1. 客户端携带 Token 请求 Gateway
2. Gateway 验证 Token（Redis）
3. Gateway 获取用户上下文和权限
4. Gateway 将用户上下文编码到请求头（`X-User-Context`）
5. Gateway 生成签名（`X-Gateway-Signature`）
6. 微服务验证签名并解析用户上下文
7. 微服务设置 `UserContext` 到 ThreadLocal

### 内部服务调用流程（服务间通信）

```
服务A → Feign拦截器添加签名 → 服务B验证签名 → 放行（不需要用户上下文）
```

1. 服务A通过 OpenFeign 调用服务B
2. Feign 拦截器自动添加内部服务签名（`X-Internal-Service`, `X-Internal-Signature`）
3. 服务B验证签名和服务白名单
4. 验证通过后直接放行，不需要用户上下文

### 双通道认证机制

Synapse Security 支持**双通道认证**，区分外部请求和内部调用：

| 类型 | 来源 | 认证方式 | 权限检查 | 示例 |
|------|------|----------|----------|------|
| **外部请求** | 用户/客户端（经Gateway） | Token + Gateway签名 | ✅ 需要（@RequirePermission） | 用户访问API |
| **内部调用** | 服务间（OpenFeign） | 内部服务签名 | ❌ 不需要（内部信任） | A服务调用B服务 |

**关键设计**：
- **外部请求**：必须经过 Gateway，微服务只接受 Gateway 传递的用户上下文
- **内部调用**：使用独立的签名机制，验证签名即可，不需要用户上下文
- **安全边界**：确保外部请求无法绕过 Gateway，内部调用无需过多权限检查

## 配置说明

### 1. 安全配置

```yaml
synapse:
  security:
    enabled: true                    # 是否启用安全模块
    mode: STRICT                     # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    security-logging: true           # 是否启用安全日志
    security-log-level: INFO         # 安全日志级别
    
    # Token 配置
    token:
      prefix: "Bearer "             # Token 前缀（用于 Authorization 请求头）
      query-param: "token"           # Token 查询参数名
      header-name: "Authorization"   # Authorization 请求头名称
      x-auth-token-header: "X-Auth-Token"  # X-Auth-Token 请求头名称
    
    # Gateway 签名配置
    gateway-signature:
      enabled: true                    # 是否启用 Gateway 签名验证
      secret: "your-gateway-secret"    # Gateway 签名密钥（生产环境必须修改）
      validity-window: 300000          # 签名有效期窗口（毫秒），默认 5 分钟
      enable-context-passing: true     # 是否启用用户上下文传递
    
    # 内部服务调用配置（服务间通信）
    internal-service:
      enabled: true                    # 是否启用内部服务调用签名验证
      service-name: "iam-service"      # 当前服务名称
      secret: "your-service-secret"    # 当前服务密钥（生产环境必须修改）
      validity-window: 300000          # 签名有效期窗口（毫秒）
      allowed-services:                # 允许调用的服务白名单
        "mdm-service": "mdm-secret"
        "business-service": "business-secret"
    
    white-list:                      # 白名单路径配置
      enabled: true
      paths:
        - /api/auth/login
        - /api/auth/logout
        - /actuator/**
```

### 2. Feign 内部服务调用配置

**Feign 拦截器已统一在 `synapse-security` 模块中实现**，业务模块无需手动创建拦截器。

#### 使用步骤

1. **添加 Feign 依赖**（在业务模块的 `pom.xml` 中）：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

2. **配置内部服务调用参数**（在 `application.yml` 中）：
```yaml
synapse:
  security:
    internal-service:
      enabled: true
      service-name: "iam-service"
      secret: "your-service-secret"
      allowed-services:
        "mdm-service": "mdm-secret"
```

3. **启用 Feign 客户端**（在启动类上）：
```java
@SpringBootApplication
@EnableFeignClients  // 添加此注解
public class IAMApplication {
    public static void main(String[] args) {
        SpringApplication.run(IAMApplication.class, args);
    }
}
```

4. **拦截器自动生效**：
   - `synapse-security` 模块中的 `InternalAuthInterceptor` 会自动被 Spring 发现并注册
   - Feign 会自动扫描所有实现了 `RequestInterceptor` 接口的 Bean
   - 所有 Feign 请求会自动添加内部服务调用签名

#### 工作原理

- **自动发现**：`InternalAuthInterceptor` 使用 `@Component` 注解，Spring 会自动扫描并注册
- **条件加载**：使用 `@ConditionalOnClass` 确保只有在 Feign 依赖存在时才加载
- **配置驱动**：使用 `@ConditionalOnProperty` 确保只有在配置启用时才生效
- **统一管理**：所有业务模块共享同一个拦截器实现，避免重复代码

### 3. OAuth2.0 认证说明

OAuth2.0 认证信息通过 `AuthRequest` 中的 `OAuth2Auth` 对象传递，不需要在配置文件中配置。
OAuth2.0 客户端信息（clientId、clientSecret 等）应在业务代码中处理，然后通过 `AuthRequest` 传递给认证服务。

## 最佳实践

### 1. 业务模块职责

- **用户信息查询**：业务模块负责查询用户的角色和权限信息
- **数据完整性**：确保传入的角色和权限信息准确完整
- **业务逻辑**：处理认证成功后的业务逻辑

### 2. 安全考虑

- **敏感信息**：不要在日志中记录密码等敏感信息
- **权限验证**：定期验证用户的角色和权限信息
- **Token安全**：使用HTTPS传输，设置合理的过期时间
- **密码加密**：使用 `PasswordUtils.encode()` 加密密码，使用 `PasswordUtils.matches()` 验证密码

### 3. 性能优化

- **缓存策略**：合理设置用户会话的缓存时间
- **批量操作**：避免频繁的认证请求
- **异步处理**：对于非关键路径的认证操作，考虑异步处理

### 4. 注意事项

- **角色和权限必须传入**：认证请求必须包含完整的用户角色和权限信息，如果角色或权限为空，认证会失败
- **缓存一致性**：用户角色和权限变更后，需要清除相关缓存，可以通过 `UserSessionService.removeUserSession()` 方法清除
- **错误处理**：认证失败时，会返回具体的错误信息，业务模块需要根据错误信息进行相应处理

## 扩展开发

### 1. 添加新的认证类型

```java
// 在AuthRequest.AuthType枚举中添加新类型
public enum AuthType {
    // 现有类型...
    USERNAME_PASSWORD,
    OAUTH2_AUTHORIZATION_CODE,
    OAUTH2_CLIENT_CREDENTIALS,
    TOKEN_VALIDATION,
    REFRESH_TOKEN,
    
    /**
     * 自定义认证类型
     */
    CUSTOM_AUTH
}

// 在DefaultAuthenticationService中添加处理逻辑
private String processWithTokenService(AuthRequest request) {
    switch (request.getAuthType()) {
        // 现有case...
        case USERNAME_PASSWORD:
            return processUsernamePassword(request);
        case OAUTH2_AUTHORIZATION_CODE:
            return processOAuth2(request);
        case CUSTOM_AUTH:
            // 自定义认证逻辑
            return processCustomAuth(request);
        default:
            throw new IllegalArgumentException("不支持的认证类型: " + request.getAuthType());
    }
}
```

### 2. 扩展认证信息模型

```java
@Data
@Builder
public class CustomAuth {
    private String customField;
    private String customSecret;
    
    public boolean isValid() {
        return customField != null && !customField.trim().isEmpty()
            && customSecret != null && !customSecret.trim().isEmpty();
    }
}
```

### 3. 自定义数据权限

```java
@Component
public class CustomDataPermissionHandler {
    
    public String buildDataScope(UserContext user, String resourceType) {
        // 实现自定义的数据权限逻辑
        return customLogic(user, resourceType);
    }
}
```

## 故障排除

### 常见问题

1. **认证失败**
   - 检查认证请求信息是否完整
   - 确保角色和权限信息不为空
   - 验证TokenService配置是否正确
   - 检查Redis连接是否正常

2. **Token续期失败**
   - 验证Token是否有效
   - 检查用户会话是否过期
   - 确认Redis连接是否正常

3. **权限检查失败**
   - 检查用户角色和权限是否正确
   - 验证权限注解配置（@RequireLogin、@RequireRole、@RequirePermission）
   - 确认UserContext是否已正确设置
   - 检查PermissionAspect是否正常工作

4. **OAuth2.0认证失败**
   - 检查OAuth2.0配置是否正确
   - 验证客户端ID和密钥
   - 确认重定向URI配置

### 调试技巧

- 启用DEBUG日志级别查看认证过程
- 检查TokenService的日志输出
- 验证用户会话缓存是否正常
- 检查UserContext是否已正确设置到ThreadLocal
- 查看PermissionAspect的日志输出

## 迁移说明

### 从 Sa-Token 迁移

如果您之前使用 Sa-Token，需要做以下迁移：

1. **替换注解**
   - `@SaCheckLogin` → `@RequireLogin`
   - `@SaCheckRole` → `@RequireRole`
   - `@SaCheckPermission` → `@RequirePermission`

2. **替换Token获取方式**
   - `StpUtil.getTokenValue()` → 从请求中获取（通过 `UserContextInterceptor` 设置的请求属性）
   - 使用 `getTokenFromRequest(HttpServletRequest request)` 方法

3. **替换权限检查方式**
   - `StpUtil.isLogin()` → `permissionService.checkLogin()` 或 `UserContext.getCurrentUser() != null`
   - `StpUtil.hasPermission()` → `permissionService.checkPermission()`
   - `StpUtil.hasRole()` → `permissionService.checkRole()`

4. **移除Sa-Token配置**
   - 移除 `sa-token.*` 配置
   - 使用 `synapse.security.*` 配置

5. **更新依赖**
   - 移除 Sa-Token 相关依赖
   - 确保已添加 `synapse-security` 依赖

## 总结

Synapse Security 模块通过自研的 TokenService 和 PermissionService，为业务模块提供了简单易用的认证和权限管理服务。所有认证类型都通过统一的 TokenService 处理，既保证了系统的统一性，又简化了架构设计。业务模块只需要关注业务逻辑，认证和权限的复杂性由Security模块统一处理。

## 相关文档

- [认证实现文档](../../../infrastructure-module/gateway-service/AUTHENTICATION_IMPLEMENTATION.md) - Gateway 层认证实现
- [配置示例](./application-security.yml) - 完整的配置示例文件
- [配置示例（简化版）](./application-security-example.yml) - 简化的配置示例
