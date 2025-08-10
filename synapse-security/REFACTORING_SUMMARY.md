# Synapse Security 模块重构总结

## 重构概述

本次重构主要解决了以下架构问题：
1. 认证策略中硬编码角色和权限的问题
2. 认证策略与UserSessionService职责不清晰的问题
3. 用户信息管理流程不清晰的问题

## 重构内容

### 1. 认证策略重构

#### SaTokenAuthenticationStrategy
- **问题**: 硬编码角色和权限（`List.of("admin")`, `List.of("admin")`）
- **解决**: 从业务模块接收角色和权限信息，通过UserSessionService进行缓存管理
- **变更**:
  - 添加 `UserSessionService` 依赖
  - 验证业务模块传入的用户角色和权限信息
  - 使用传入的角色和权限创建UserContext
  - 通过UserSessionService存储用户会话、角色和权限信息
  - Token续期时从UserSessionService获取最新信息

#### OAuth2AuthenticationStrategy
- **问题**: 硬编码角色和权限（`List.of("user")`, `List.of("user:read")`）
- **解决**: 与SaTokenAuthenticationStrategy保持一致的重构
- **变更**: 同上

### 2. 认证请求模型重构

#### AuthRequest
- **新增字段**:
  - `List<String> roles`: 用户角色列表（由业务模块传入）
  - `List<String> permissions`: 用户权限列表（由业务模块传入）
- **目的**: 让业务模块能够传入完整的用户信息

### 3. 删除冗余接口

#### UserService
- **删除原因**: 在当前架构中无法找到使用场景
- **替代方案**: 通过UserSessionService进行用户信息缓存管理

## 重构后的架构流程

### 登录阶段
```
业务模块查询用户信息 → 传入Security模块 → 写入缓存
```

1. 业务模块查询用户的账户、角色、权限等信息
2. 通过AuthRequest传入Security模块
3. 认证策略验证传入信息的完整性
4. 通过UserSessionService存储到缓存中

### 后续访问阶段
```
通过UserSessionService获取缓存信息 → 进行认证判定
```

1. 从缓存获取用户会话信息
2. 从缓存获取用户角色和权限信息
3. 进行权限检查和角色验证

## 架构优势

### 1. 职责清晰
- **业务模块**: 负责用户信息查询和业务逻辑
- **Security模块**: 负责认证流程和权限管理
- **UserSessionService**: 负责用户信息的缓存管理

### 2. 数据一致性
- 角色和权限信息由业务模块统一管理
- 避免了硬编码导致的数据不一致问题

### 3. 扩展性
- 支持动态角色和权限分配
- 便于后续添加新的认证方式

### 4. 缓存管理
- 统一的用户信息缓存入口
- 支持会话续期和权限刷新

## 使用示例

### 业务模块调用认证
```java
// 查询用户信息
User user = userRepository.findByUsername(username);
List<String> roles = roleService.getUserRoles(user.getId());
List<String> permissions = permissionService.getUserPermissions(user.getId());

// 构建认证请求
AuthRequest authRequest = AuthRequest.builder()
    .username(username)
    .password(password)
    .userId(user.getId().toString())
    .roles(roles)
    .permissions(permissions)
    .build();

// 调用认证策略
Result<AuthResponse> result = authenticationStrategy.authenticate(authRequest);
```

### 权限检查
```java
// 从缓存获取用户权限
List<String> permissions = userSessionService.getUserPermissions(token);
boolean hasPermission = permissions.contains("user:read");

// 检查用户角色
List<String> roles = userSessionService.getUserRoles(token);
boolean hasRole = roles.contains("admin");
```

## 总结

本次重构成功解决了硬编码问题，建立了清晰的职责分离架构：
- 业务模块负责用户信息查询
- Security模块负责认证流程
- UserSessionService负责缓存管理

这样的架构更加清晰、可维护，并且为后续的功能扩展奠定了良好的基础。 