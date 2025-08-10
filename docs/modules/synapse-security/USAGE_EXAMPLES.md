# Synapse Security 使用示例

## 概述

本文档说明如何在业务模块中使用 Synapse Security 模块进行用户认证和权限管理。

## 认证流程

### 1. 登录认证

业务模块需要先查询用户的完整信息，然后调用认证策略进行认证。

#### 步骤1: 查询用户信息
```java
@Service
@RequiredArgsConstructor
public class UserAuthService {
    
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;
    
    public Result<AuthResponse> login(String username, String password, String clientIp) {
        // 1. 查询用户基本信息
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 2. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() == UserStatus.LOCKED) {
            return Result.error("用户账号被锁定");
        }
        
        if (user.getStatus() == UserStatus.DISABLED) {
            return Result.error("用户账号被禁用");
        }
        
        // 4. 查询用户角色
        List<String> roles = roleService.getUserRoles(user.getId());
        
        // 5. 查询用户权限
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 6. 构建认证请求
        AuthRequest authRequest = AuthRequest.builder()
            .username(username)
            .password(password)
            .userId(user.getId().toString())
            .roles(roles)
            .permissions(permissions)
            .clientIp(clientIp)
            .build();
        
        // 7. 调用认证策略
        AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy("satoken");
        Result<AuthResponse> result = strategy.authenticate(authRequest);
        
        if (result.isSuccess()) {
            // 8. 更新用户最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(clientIp);
            userRepository.save(user);
        }
        
        return result;
    }
}
```

#### 步骤2: 认证策略处理
认证策略会：
1. 验证传入的角色和权限信息
2. 创建用户上下文
3. 通过TokenManager生成token
4. 通过UserSessionService存储到缓存

### 2. OAuth2.0认证

#### 步骤1: 处理OAuth2.0回调
```java
@Service
@RequiredArgsConstructor
public class OAuth2AuthService {
    
    private final OAuth2Client oAuth2Client;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;
    
    public Result<AuthResponse> handleOAuth2Callback(String code, String provider) {
        // 1. 通过授权码获取用户信息
        OAuth2UserInfo oauth2User = oAuth2Client.getUserInfo(code, provider);
        
        // 2. 查找或创建本地用户
        User user = findOrCreateUser(oauth2User);
        
        // 3. 查询用户角色和权限
        List<String> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 4. 构建认证请求
        AuthRequest authRequest = AuthRequest.builder()
            .username(user.getUsername())
            .userId(user.getId().toString())
            .provider(provider)
            .roles(roles)
            .permissions(permissions)
            .build();
        
        // 5. 调用OAuth2认证策略
        AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy("oauth2");
        return strategy.authenticate(authRequest);
    }
    
    private User findOrCreateUser(OAuth2UserInfo oauth2User) {
        // 根据OAuth2用户信息查找或创建本地用户
        // 这里省略具体实现
        return user;
    }
}
```

## 权限检查

### 1. 从缓存获取权限信息
```java
@Service
@RequiredArgsConstructor
public class PermissionCheckService {
    
    private final UserSessionService userSessionService;
    
    public boolean hasPermission(String token, String permission) {
        return userSessionService.hasPermission(token, permission);
    }
    
    public boolean hasRole(String token, String role) {
        return userSessionService.hasRole(token, role);
    }
    
    public List<String> getUserPermissions(String token) {
        return userSessionService.getUserPermissions(token);
    }
    
    public List<String> getUserRoles(String token) {
        return userSessionService.getUserRoles(token);
    }
}
```

### 2. 在Controller中使用
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final PermissionCheckService permissionCheckService;
    
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        // 检查权限
        if (!permissionCheckService.hasPermission(token, "user:read")) {
            return Result.error("权限不足");
        }
        
        // 业务逻辑
        User user = userService.findById(id);
        return Result.success(user);
    }
    
    @PostMapping
    public Result<User> createUser(@RequestBody User user, @RequestHeader("Authorization") String token) {
        // 检查角色
        if (!permissionCheckService.hasRole(token, "admin")) {
            return Result.error("需要管理员权限");
        }
        
        // 业务逻辑
        User createdUser = userService.createUser(user);
        return Result.success(createdUser);
    }
}
```

## Token管理

### 1. Token续期
```java
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final AuthenticationStrategy authenticationStrategy;
    
    public Result<AuthResponse> renewToken(String token) {
        return authenticationStrategy.renewToken(token);
    }
}
```

### 2. Token撤销
```java
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final TokenManager tokenManager;
    
    public void logout(String token) {
        tokenManager.revokeToken(token);
    }
}
```

## 配置示例

### 1. 认证策略工厂
```java
@Component
public class AuthenticationStrategyFactory {
    
    private final Map<String, AuthenticationStrategy> strategies;
    
    public AuthenticationStrategyFactory(List<AuthenticationStrategy> strategyList) {
        strategies = strategyList.stream()
            .collect(Collectors.toMap(AuthenticationStrategy::getStrategyType, Function.identity()));
    }
    
    public AuthenticationStrategy getStrategy(String type) {
        AuthenticationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的认证策略类型: " + type);
        }
        return strategy;
    }
}
```

### 2. 业务模块配置
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public AuthenticationStrategyFactory authenticationStrategyFactory(
            List<AuthenticationStrategy> strategies) {
        return new AuthenticationStrategyFactory(strategies);
    }
}
```

## 注意事项

### 1. 角色和权限必须传入
- 认证请求必须包含完整的用户角色和权限信息
- 如果角色或权限为空，认证会失败

### 2. 缓存一致性
- 用户角色和权限变更后，需要清除相关缓存
- 可以通过UserSessionService的removeUserSession方法清除

### 3. 错误处理
- 认证失败时，会返回具体的错误信息
- 业务模块需要根据错误信息进行相应处理

### 4. 安全性
- 密码验证应该在业务模块进行
- 认证策略只负责token生成和缓存管理
- 敏感信息不要记录在日志中

## 总结

通过这种架构设计：
1. **业务模块**负责用户信息查询和业务逻辑
2. **Security模块**负责认证流程和权限管理
3. **UserSessionService**负责用户信息的缓存管理

这样的设计使得职责更加清晰，代码更易维护，也为后续的功能扩展奠定了良好的基础。 