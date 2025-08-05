# Sa-Token 使用指南

## 概述

本框架已优化为充分利用 Sa-Token 的功能，避免重复造轮子。请使用 Sa-Token 的注解和工具方法进行权限控制。

## 权限检查注解

### 1. 登录检查

```java
import cn.dev33.satoken.annotation.SaCheckLogin;

@RestController
public class UserController {
    
    @SaCheckLogin
    @GetMapping("/user/profile")
    public Result<UserInfo> getUserProfile() {
        // 只有登录用户才能访问
        return Result.success(userService.getCurrentUserProfile());
    }
}
```

### 2. 权限检查

```java
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
public class UserController {
    
    @SaCheckPermission("user:read")
    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        // 只有具有 user:read 权限的用户才能访问
        return Result.success(userService.getAllUsers());
    }
    
    @SaCheckPermission({"user:create", "user:write"})
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        // 只有具有 user:create 或 user:write 权限的用户才能访问
        return Result.success(userService.createUser(user));
    }
}
```

### 3. 角色检查

```java
import cn.dev33.satoken.annotation.SaCheckRole;

@RestController
public class AdminController {
    
    @SaCheckRole("admin")
    @GetMapping("/admin/dashboard")
    public Result<DashboardData> getDashboard() {
        // 只有 admin 角色的用户才能访问
        return Result.success(adminService.getDashboardData());
    }
    
    @SaCheckRole({"admin", "super_admin"})
    @PostMapping("/admin/users")
    public Result<User> createAdminUser(@RequestBody User user) {
        // 只有 admin 或 super_admin 角色的用户才能访问
        return Result.success(adminService.createAdminUser(user));
    }
}
```

### 4. 安全校验

```java
import cn.dev33.satoken.annotation.SaCheckSafe;

@RestController
public class SensitiveController {
    
    @SaCheckSafe("password")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        // 需要密码二次验证
        return Result.success(userService.changePassword(request));
    }
}
```

## 编程式权限检查

### 1. 使用 StpUtil

```java
import cn.dev33.satoken.stp.StpUtil;

@RestController
public class UserController {
    
    @GetMapping("/user/info")
    public Result<UserInfo> getUserInfo() {
        // 检查是否登录
        if (!StpUtil.isLogin()) {
            return Result.error("请先登录");
        }
        
        // 检查权限
        if (!StpUtil.hasPermission("user:read")) {
            return Result.error("没有读取权限");
        }
        
        // 检查角色
        if (!StpUtil.hasRole("user")) {
            return Result.error("没有用户角色");
        }
        
        // 获取当前用户ID
        String userId = StpUtil.getLoginId().toString();
        
        return Result.success(userService.getUserInfo(userId));
    }
}
```

### 2. 使用 SaRouter

```java
import cn.dev33.satoken.router.SaRouter;

@RestController
public class UserController {
    
    @GetMapping("/user/data")
    public Result<UserData> getUserData() {
        // 链式调用，任何一个失败都会抛出异常
        SaRouter.match(StpUtil.isLogin())
                .check(r -> StpUtil.hasPermission("user:read"))
                .check(r -> StpUtil.hasRole("user"))
                .stop();
        
        // 如果所有检查都通过，执行业务逻辑
        return Result.success(userService.getUserData());
    }
}
```

## 异常处理

Sa-Token 会自动处理权限检查失败的情况，抛出相应的异常。你可以在全局异常处理器中捕获这些异常：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        return Result.error("请先登录");
    }
    
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        return Result.error("没有权限: " + e.getPermission());
    }
    
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        return Result.error("没有角色: " + e.getRole());
    }
}
```

## 最佳实践

### 1. 优先使用注解

```java
// ✅ 推荐：使用注解
@SaCheckPermission("user:read")
public Result<List<User>> getUsers() {
    return Result.success(userService.getAllUsers());
}

// ❌ 不推荐：编程式检查
public Result<List<User>> getUsers() {
    if (!StpUtil.hasPermission("user:read")) {
        return Result.error("没有权限");
    }
    return Result.success(userService.getAllUsers());
}
```

### 2. 合理设计权限标识

```java
// ✅ 推荐：清晰的权限标识
@SaCheckPermission("user:read")
@SaCheckPermission("user:create")
@SaCheckPermission("user:update")
@SaCheckPermission("user:delete")

// ❌ 不推荐：模糊的权限标识
@SaCheckPermission("user")
```

### 3. 使用组合注解

```java
// ✅ 推荐：组合使用多个注解
@SaCheckLogin
@SaCheckRole("admin")
@SaCheckPermission("user:manage")
public Result<Void> manageUsers() {
    // 业务逻辑
}
```

## 注意事项

1. **不要重复造轮子**：框架已经优化，删除了重复的权限检查方法，请使用 Sa-Token 的原生功能。

2. **用户上下文**：`UserContextInterceptor` 只负责设置用户上下文，不进行权限检查。

3. **权限数据**：权限和角色数据通过 `PermissionManager` 从 Redis 中获取，确保登录时正确存储。

4. **拦截器顺序**：用户上下文拦截器优先级为1，Sa-Token拦截器优先级为2，确保用户上下文先设置。

5. **异常处理**：建议配置全局异常处理器来统一处理 Sa-Token 的异常。 