# Synapse Security 使用示例

## 概述

本文档提供了 Synapse Security 模块的使用示例，包括认证、权限控制、数据权限等功能的使用方法。

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
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
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
public class TokenService {
    
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
    
    private final AuthenticationService authenticationService;
    
    public Result<Void> logout() {
        return authenticationService.logout();
    }
}
```

## 权限控制使用

### 1. 注解权限检查

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @SaCheckLogin
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        // 获取当前用户信息
        UserContext currentUser = authenticationService.getCurrentUser();
        return Result.success(userService.getProfile(currentUser.getUserId()));
    }
    
    @SaCheckPermission("user:read")
    @GetMapping("/{userId}")
    public Result<UserInfo> getUserInfo(@PathVariable String userId) {
        return Result.success(userService.getUserInfo(userId));
    }
    
    @SaCheckRole("admin")
    @PostMapping("/create")
    public Result<UserInfo> createUser(@RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }
}
```

### 2. 编程式权限检查

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final PermissionManager permissionManager;
    
    public void updateUser(String userId, UpdateUserRequest request) {
        // 检查当前用户是否有权限修改指定用户
        if (!permissionManager.hasPermission("user:update", userId)) {
            throw new AccessDeniedException("没有权限修改用户信息");
        }
        
        // 执行更新逻辑
        userRepository.updateUser(userId, request);
    }
}
```

## 数据权限使用

### 1. 数据权限规则配置

```java
@Service
@RequiredArgsConstructor
public class DataPermissionService {
    
    private final DataPermissionService dataPermissionService;
    
    public List<User> getUsersWithPermission() {
        UserContext currentUser = authenticationService.getCurrentUser();
        
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
}
```

## 配置示例

### 1. 基础配置

```yaml
# 安全模块配置
synapse:
  security:
    enabled: true
    mode: STRICT
    security-logging: true
    security-log-level: INFO

# Sa-Token配置
sa-token:
  token-name: Authorization
  timeout: 2592000
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true
```

### 2. OAuth2.0配置

```yaml
# OAuth2.0配置
synapse:
  oauth2:
    enabled: true
    providers:
      github:
        client-id: ${GITHUB_CLIENT_ID}
        client-secret: ${GITHUB_CLIENT_SECRET}
        redirect-uri: http://localhost:8080/oauth2/callback
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
- 不要在日志中记录敏感信息
- 使用HTTPS传输Token
- 定期验证用户权限信息 