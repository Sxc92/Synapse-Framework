# Synapse Core 模块

核心模块提供 Synapse Framework 的基础功能和通用工具。

## 🎯 核心功能

### 统一响应处理
- **Result<T>** - 标准化 API 响应格式
- **全局异常处理** - 统一异常响应
- **国际化支持** - 多语言消息

### 基础工具类
- **DateUtils** - 日期时间工具
- **StringUtils** - 字符串工具
- **CollectionUtils** - 集合工具

### 配置管理
- **自动配置** - Spring Boot 自动配置
- **配置验证** - 配置属性验证
- **环境配置** - 多环境配置支持

## 📖 使用指南

### 统一响应格式

```java
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return Result.success(user);
        } catch (UserNotFoundException e) {
            return Result.error("USER_NOT_FOUND", "用户不存在");
        }
    }
}
```

### 国际化消息

```java
@Autowired
private MessageSource messageSource;

public String getMessage(String code, Object... args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
}
```

## 🔧 配置选项

```yaml
synapse:
  core:
    # 国际化配置
    i18n:
      default-locale: zh_CN
      message-basename: i18n/messages
      encoding: UTF-8
      
    # 异常处理配置
    exception:
      show-details: false
      log-level: WARN
```

## 📚 相关文档

- [快速开始](../QUICKSTART.md)
- [架构设计](../ARCHITECTURE.md)
- [配置指南](../CONFIGURATION.md)
