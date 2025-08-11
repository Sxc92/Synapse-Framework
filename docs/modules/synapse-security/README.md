# Synapse Security 模块

## 概述

Synapse Security 模块是 Synapse Framework 的安全认证和授权模块，提供了完整的身份认证、权限控制和安全管理功能。基于 Sa-Token 框架，支持多种认证方式和细粒度的权限控制。

## 主要特性

- 🔐 **多种认证方式**：JWT、Session、Token 等
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
```

### 3. 使用示例

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        // 登录
        StpUtil.login(request.getUsername());
        return Result.success("登录成功");
    }
    
    @SaCheckLogin
    @GetMapping("/info")
    public Result<UserInfo> getUserInfo() {
        // 获取当前登录用户信息
        String username = StpUtil.getLoginIdAsString();
        return Result.success(userService.getUserInfo(username));
    }
    
    @SaCheckPermission("user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
```

## 配置说明

### 1. 认证配置

**JWT 配置**
```yaml
sa-token:
  # JWT 配置
  jwt-secret-key: your-secret-key
  # JWT 有效期
  jwt-timeout: 2592000
  # JWT 临时有效期
  jwt-activity-timeout: -1
```

**Session 配置**
```yaml
sa-token:
  # Session 配置
  is-read-cookie: false
  is-read-header: true
  is-read-body: false
  is-write-cookie: false
```

### 2. 权限配置

**权限码配置**
```yaml
sa-token:
  # 权限码配置
  permission-code: "user:add,user:delete,user:update,user:query"
  # 角色码配置
  role-code: "admin,user,guest"
```

**注解配置**
```java
// 登录验证
@SaCheckLogin

// 权限验证
@SaCheckPermission("user:add")

// 角色验证
@SaCheckRole("admin")

// 安全注解
@SaCheckSafe
```

### 3. 安全配置

**XSS 防护**
```yaml
synapse:
  security:
    xss:
      enabled: true
      exclude-paths: "/api/public/**"
```

**CSRF 防护**
```yaml
synapse:
  security:
    csrf:
      enabled: true
      token-header: "X-CSRF-TOKEN"
```

## 高级功能

### 1. 多端登录管理

```java
@Service
public class UserService {
    
    public void login(String username, String device) {
        // 登录并指定设备类型
        StpUtil.login(username, device);
    }
    
    public void kickout(String username, String device) {
        // 踢出指定设备的登录
        StpUtil.kickout(username, device);
    }
    
    public List<String> getLoginDevices(String username) {
        // 获取用户所有登录设备
        return StpUtil.getLoginDevices(username);
    }
}
```

### 2. 权限拦截器

```java
@Component
public class SaInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        // 权限验证逻辑
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            
            // 检查登录状态
            if (method.hasMethodAnnotation(SaCheckLogin.class)) {
                StpUtil.checkLogin();
            }
            
            // 检查权限
            SaCheckPermission permission = method.getMethodAnnotation(SaCheckPermission.class);
            if (permission != null) {
                StpUtil.checkPermission(permission.value());
            }
        }
        
        return true;
    }
}
```

### 3. 操作日志记录

```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {
    
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String username = StpUtil.getLoginIdAsString();
        
        try {
            Object result = point.proceed();
            long endTime = System.currentTimeMillis();
            
            // 记录操作日志
            log.info("用户[{}]执行操作[{}]成功，耗时: {}ms", 
                username, operationLog.value(), endTime - startTime);
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            
            // 记录错误日志
            log.error("用户[{}]执行操作[{}]失败，耗时: {}ms，错误: {}", 
                username, operationLog.value(), endTime - startTime, e.getMessage());
            
            throw e;
        }
    }
}
```

## 最佳实践

### 1. 权限设计

- 使用 RBAC 模型设计权限
- 权限码命名规范：`模块:操作`
- 角色继承关系清晰

### 2. 安全防护

- 启用 XSS 和 CSRF 防护
- 使用 HTTPS 传输
- 定期更新密钥

### 3. 登录管理

- 设置合理的 Token 过期时间
- 实现记住我功能
- 支持多端登录

### 4. 审计日志

- 记录所有关键操作
- 日志信息完整准确
- 支持日志查询和分析

## 故障排除

### 常见问题

1. **Token 过期**
   - 检查 Token 有效期配置
   - 实现自动续期机制

2. **权限验证失败**
   - 检查权限码配置
   - 验证用户角色分配

3. **登录状态丢失**
   - 检查 Cookie 配置
   - 验证 Token 存储方式

### 日志配置

```yaml
logging:
  level:
    com.indigo.security: DEBUG
    cn.dev33.satoken: DEBUG
```

## 版本历史

| 版本 | 更新内容 |
|------|----------|
| 1.0.0 | 初始版本，基础认证功能 |
| 1.1.0 | 添加权限控制功能 |
| 1.2.0 | 集成 Sa-Token 框架 |
| 1.3.0 | 添加安全防护功能 |
| 1.4.0 | 优化性能和稳定性 |

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。 