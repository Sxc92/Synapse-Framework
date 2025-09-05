# Synapse Security 认证门面使用指南

## 概述

认证门面（Authentication Facade）是 Synapse Security 模块的核心组件，它提供了一个统一的接口来处理不同类型的认证请求。认证门面直接使用Sa-Token框架，能够处理用户名密码、OAuth2.0、Token验证等多种认证方式，所有认证最终都通过Sa-Token框架统一管理。

## 核心特性

### 1. 统一Sa-Token框架
- **单一框架**：所有认证都通过Sa-Token框架处理
- **无需策略选择**：直接调用Sa-Token API，架构更简单
- **统一管理**：Sa-Token统一管理所有认证方式

### 2. 统一的认证接口
- **单一入口**：`AuthenticationService.authenticate()`
- **统一响应**：所有认证方式返回相同格式的响应
- **错误处理**：统一的异常处理和错误信息

### 3. 业务信息集成
- **角色权限管理**：业务模块传入用户的角色和权限信息
- **缓存管理**：通过 UserSessionService 管理用户会话信息
- **数据一致性**：确保认证信息与业务数据保持一致

## 架构设计

### 认证流程

```
业务模块 → AuthRequest → 认证门面 → Sa-Token框架 → 认证结果
    ↓           ↓           ↓           ↓           ↓
用户信息    认证凭证    统一接口    统一处理    统一响应
```

### 认证类型支持

| 认证类型 | Sa-Token处理方式 | 说明 |
|---------|----------------|------|
| USERNAME_PASSWORD | `StpUtil.login(userId)` | 用户名密码认证 |
| OAUTH2_AUTHORIZATION_CODE | `StpUtil.login(userId)` | OAuth2.0授权码模式 |
| OAUTH2_CLIENT_CREDENTIALS | `StpUtil.login(userId)` | OAuth2.0客户端凭证模式 |
| TOKEN_VALIDATION | `StpUtil.checkLogin()` | Token验证 |
| REFRESH_TOKEN | `StpUtil.renewTimeout()` | 刷新Token |

## 使用方法

### 1. 基础认证请求

```java
// 用户名密码认证
AuthRequest request = AuthRequest.builder()
    .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
    .usernamePasswordAuth(UsernamePasswordAuth.builder()
        .username("admin")
        .password("password123")
        .build())
    .userId("1001")
    .roles(Arrays.asList("admin", "user"))
    .permissions(Arrays.asList("user:read", "user:write", "admin:all"))
    .build();

// 调用认证服务
Result<AuthResponse> result = authenticationService.authenticate(request);
```

### 2. OAuth2.0认证请求

```java
// OAuth2.0授权码模式
AuthRequest request = AuthRequest.builder()
    .authType(AuthRequest.AuthType.OAUTH2_AUTHORIZATION_CODE)
    .oauth2Auth(OAuth2Auth.builder()
        .clientId("client123")
        .clientSecret("secret456")
        .code("auth_code_789")
        .redirectUri("http://localhost:8080/callback")
        .provider("github")
        .build())
    .userId("2001")
    .roles(Arrays.asList("user"))
    .permissions(Arrays.asList("user:read"))
    .build();

Result<AuthResponse> result = authenticationService.authenticate(request);
```

### 3. Token续期

```java
// Token续期
String token = "your-token-here";
Result<AuthResponse> result = authenticationService.renewToken(token);
```

### 4. 获取当前用户

```java
// 获取当前登录用户信息
UserContext currentUser = authenticationService.getCurrentUser();
if (currentUser != null) {
    System.out.println("当前用户: " + currentUser.getUsername());
    System.out.println("用户角色: " + currentUser.getRoles());
}
```

### 5. 用户登出

```java
// 用户登出
Result<Void> result = authenticationService.logout();
if (result.isSuccess()) {
    System.out.println("登出成功");
}
```

## 配置说明

### 安全配置

```yaml
synapse:
  security:
    # 是否启用安全模块
    enabled: true
    # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    mode: STRICT
    # 是否启用安全日志
    security-logging: true
    # 安全日志级别
    security-log-level: INFO
```

### Sa-Token配置

```yaml
# Sa-Token基础配置
sa-token:
  # Token名称
  token-name: Authorization
  # Token有效期（秒）
  timeout: 2592000
  # Token最低活跃频率（秒）
  active-timeout: -1
  # 是否允许同一账号多地同时登录
  is-concurrent: true
  # 是否共用一个Token
  is-share: false
  # Token风格
  token-style: uuid
  # 是否输出操作日志
  is-log: true

# Sa-Token OAuth2.0配置
sa-token:
  # OAuth2.0配置
  oauth2:
    # 是否启用OAuth2.0
    enabled: true
    # 授权码有效期（秒）
    code-timeout: 300
    # 访问令牌有效期（秒）
    access-token-timeout: 7200
```

## 最佳实践

### 1. 业务模块职责

- **用户信息查询**：业务模块负责查询用户的角色和权限信息
- **数据完整性**：确保传入的角色和权限信息准确完整
- **业务逻辑**：处理认证成功后的业务逻辑

### 2. 安全考虑

- **敏感信息**：不要在日志中记录密码等敏感信息
- **权限验证**：定期验证用户的角色和权限信息
- **Token安全**：使用HTTPS传输，设置合理的过期时间

### 3. 性能优化

- **缓存策略**：合理设置用户会话的缓存时间
- **批量操作**：避免频繁的认证请求
- **异步处理**：对于非关键路径的认证操作，考虑异步处理

## 扩展开发

### 1. 添加新的认证类型

```java
// 在AuthRequest.AuthType枚举中添加新类型
public enum AuthType {
    // 现有类型...
    
    /**
     * 自定义认证类型
     */
    CUSTOM_AUTH
}

// 在DefaultAuthenticationService中添加处理逻辑
private String processWithSaToken(AuthRequest request) {
    switch (request.getAuthType()) {
        // 现有case...
        
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
    
    public boolean isValid() {
        return customField != null && !customField.trim().isEmpty();
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

3. **OAuth2.0认证失败**
   - 检查OAuth2.0配置是否正确
   - 验证客户端ID和密钥
   - 确认重定向URI配置

### 调试技巧

- 启用DEBUG日志级别查看认证过程
- 检查Sa-Token的配置和状态
- 验证用户会话缓存是否正常

## 总结

认证门面通过直接使用Sa-Token框架，为业务模块提供了简单易用的认证服务。所有认证类型都通过统一的Sa-Token框架处理，既保证了系统的统一性，又简化了架构设计。业务模块只需要关注业务逻辑，认证的复杂性由认证门面统一处理。
