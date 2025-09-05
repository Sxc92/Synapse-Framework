# Synapse Security 模块

## 概述

Synapse Security 模块是 Synapse Framework 的安全认证和授权模块，提供了完整的身份认证、权限控制和安全管理功能。基于 Sa-Token 框架，支持多种认证方式和细粒度的权限控制。

## 主要特性

- 🔐 **多种认证方式**：用户名密码、OAuth2.0、Token验证等
- 🛡️ **权限控制**：基于注解的权限验证
- 🔑 **角色管理**：支持多角色和角色继承
- 🚪 **登录管理**：记住我、踢人下线、账号封禁
- 📱 **多端支持**：Web、移动端、小程序等
- 🔒 **安全防护**：XSS、CSRF、SQL注入防护
- 📊 **操作审计**：完整的操作日志记录

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
# Sa-Token 配置
sa-token:
  # token 名称（同时也是 cookie 名称）
  token-name: Authorization
  # token 有效期（单位：秒） 默认30天，-1 代表永久有效
  timeout: 2592000
  # token 最低活跃频率（单位：秒），如果 token 超过此时间没有访问系统就会被冻结，默认-1 代表不限制，永不冻结
  active-timeout: -1
  # 是否允许同一账号多地同时登录 （为 true 时允许一起登录, 为 false 时新登录挤掉旧登录）
  is-concurrent: true
  # 在多人登录同一账号时，是否共用一个 token （为 true 时所有登录共用一个 token, 为 false 时每次登录新建一个 token）
  is-share: false
  # token 风格（默认可取值：uuid、simple-uuid、random-32、random-64、random-128、tik）
  token-style: uuid
  # 是否输出操作日志
  is-log: true

# Synapse Security 配置
synapse:
  security:
    enabled: true
    mode: STRICT
    security-logging: true
    security-log-level: INFO
```

### 3. 使用示例

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
    
    @SaCheckLogin
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        UserContext currentUser = authenticationService.getCurrentUser();
        return Result.success(userService.getProfile(currentUser.getUserId()));
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

### 2. TokenManager
基于Sa-Token的Token管理服务，提供：
- 用户登录
- Token验证
- Token续期
- 用户会话管理

### 3. PermissionManager
权限管理服务，支持：
- 角色权限检查
- 注解权限验证
- 动态权限管理

### 4. DataPermissionService
数据权限服务，支持：
- 数据范围控制
- 自定义权限规则
- 多维度权限控制

## 认证流程

```
业务模块查询用户信息 → 传入Security模块 → Sa-Token框架处理 → 返回Token
```

1. 业务模块查询用户的账户、角色、权限等信息
2. 通过AuthRequest传入Security模块
3. 认证服务使用Sa-Token框架处理认证
4. 通过UserSessionService存储到缓存中

## 权限控制

### 1. 注解权限检查

```java
@SaCheckLogin                    // 检查是否登录
@SaCheckPermission("user:read")  // 检查是否有指定权限
@SaCheckRole("admin")            // 检查是否有指定角色
```

### 2. 编程式权限检查

```java
@Autowired
private PermissionManager permissionManager;

public void updateUser(String userId) {
    if (!permissionManager.hasPermission("user:update", userId)) {
        throw new AccessDeniedException("没有权限修改用户信息");
    }
    // 执行更新逻辑
}
```

## 数据权限

### 1. 数据范围控制

```java
@Autowired
private DataPermissionService dataPermissionService;

public List<User> getUsers() {
    UserContext currentUser = authenticationService.getCurrentUser();
    String dataScope = dataPermissionService.getDataScope(currentUser, "user");
    return userRepository.findByDataScope(dataScope);
}
```

### 2. 自定义权限规则

```java
@Component
public class CustomDataPermissionHandler {
    
    public String buildDataScope(UserContext user, String resourceType) {
        if (user.hasRole("SUPER_ADMIN")) {
            return "1=1"; // 超级管理员可以查看所有数据
        }
        
        if (user.hasRole("ADMIN")) {
            return "dept_id = " + user.getDeptId(); // 管理员只能查看本部门数据
        }
        
        return "create_user_id = " + user.getUserId(); // 普通用户只能查看自己创建的数据
    }
}
```

## 配置说明

### 1. 安全配置

```yaml
synapse:
  security:
    enabled: true                    # 是否启用安全模块
    mode: STRICT                     # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    security-logging: true           # 是否启用安全日志
    security-log-level: INFO         # 安全日志级别
```

### 2. OAuth2.0配置

```yaml
synapse:
  oauth2:
    enabled: true
    providers:
      github:
        client-id: ${GITHUB_CLIENT_ID}
        client-secret: ${GITHUB_CLIENT_SECRET}
        redirect-uri: http://localhost:8080/oauth2/callback
```

## 最佳实践

### 1. 业务模块职责
- 查询用户信息（账户、角色、权限）
- 传入完整的用户信息到Security模块
- 处理认证成功后的业务逻辑

### 2. 安全考虑
- 不要在日志中记录密码等敏感信息
- 使用HTTPS传输Token
- 定期验证用户的角色和权限信息

### 3. 性能优化
- 合理设置用户会话的缓存时间
- 避免频繁的认证请求
- 使用批量权限检查

## 扩展开发

### 1. 添加新的认证类型

```java
// 在AuthRequest.AuthType枚举中添加新类型
public enum AuthType {
    // 现有类型...
    CUSTOM_AUTH
}

// 在DefaultAuthenticationService中添加处理逻辑
private String processWithSaToken(AuthRequest request) {
    switch (request.getAuthType()) {
        // 现有case...
        case CUSTOM_AUTH:
            return processCustomAuth(request);
        default:
            throw new IllegalArgumentException("不支持的认证类型: " + request.getAuthType());
    }
}
```

### 2. 自定义数据权限

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
   - 验证Sa-Token配置是否正确

2. **Token续期失败**
   - 验证Token是否有效
   - 检查用户会话是否过期
   - 确认Sa-Token配置

3. **权限检查失败**
   - 检查用户角色和权限是否正确
   - 验证权限注解配置
   - 确认权限规则配置

### 调试技巧

- 启用DEBUG日志级别查看认证过程
- 检查Sa-Token的配置和状态
- 验证用户会话缓存是否正常

## 总结

Synapse Security 模块通过直接使用Sa-Token框架，为业务模块提供了简单易用的认证和权限管理服务。所有认证类型都通过统一的Sa-Token框架处理，既保证了系统的统一性，又简化了架构设计。业务模块只需要关注业务逻辑，认证和权限的复杂性由Security模块统一处理。
