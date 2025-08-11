# Synapse Framework 文档索引

## 📚 文档概览

欢迎使用 Synapse Framework 文档！本文档索引将帮助你快速找到所需的信息。

## 📖 核心文档

### 1. [README.md](README.md) - 框架概述
- **框架介绍**：Synapse Framework 的整体介绍和设计理念
- **模块架构**：各模块的功能和职责说明
- **技术栈**：使用的技术和框架
- **快速开始**：从零开始搭建项目
- **最佳实践**：开发规范和最佳实践

### 2. [ARCHITECTURE.md](ARCHITECTURE.md) - 架构设计
- **整体架构图**：框架的层次结构和模块关系
- **设计模式**：使用的核心设计模式
- **数据流**：请求处理和认证授权流程
- **扩展点**：自定义扩展的实现方式
- **性能优化**：性能优化策略和方案

### 3. [USAGE_GUIDE.md](USAGE_GUIDE.md) - 使用指南
- **环境准备**：开发环境搭建
- **项目创建**：创建新项目的步骤
- **功能使用**：各功能模块的详细使用方法
- **代码示例**：完整的代码示例和最佳实践
- **常见问题**：常见问题的解决方案

### 4. [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) - 开发笔记
- **会话管理重构**：会话管理模块重构的背景、方案和实现
- **分布式锁增强**：分布式锁模块的功能增强和架构优化
- **缓存翻译注解**：缓存属性翻译注解的设计方案和实现
- **架构优化总结**：设计原则、架构模式和最佳实践

## 🔧 模块文档

### 核心模块 (synapse-core)
- **统一响应格式**：`Result<T>` 的使用方法
- **异常处理**：全局异常处理机制
- **用户上下文**：`UserContext` 的管理
- **工具类库**：常用工具类的使用
- **详细文档**：[模块文档索引](MODULES_INDEX.md#4-synapse-core-模块)

### 数据库模块 (synapse-databases)
- **实体定义**：实体类的创建和配置
- **Repository 层**：数据访问层的实现
- **查询构建**：注解驱动的查询构建
- **分页查询**：分页功能的实现
- **动态数据源**：多数据源配置和使用
- **详细文档**：[模块文档索引](MODULES_INDEX.md#3-synapse-databases-模块)

### 安全模块 (synapse-security)
- **认证配置**：Sa-Token 的配置和使用
- **权限控制**：注解权限控制的实现
- **用户会话**：会话管理机制
- **Token 管理**：Token 的生成和验证
- **详细文档**：[模块文档索引](MODULES_INDEX.md#1-synapse-security-模块)
  - [使用示例](modules/synapse-security/USAGE_EXAMPLES.md)
  - [重构总结](modules/synapse-security/REFACTORING_SUMMARY.md)
  - [Sa-Token 使用指南](modules/synapse-security/SA_TOKEN_USAGE.md)

### 缓存模块 (synapse-cache)
- **Redis 配置**：Redis 连接和配置
- **缓存注解**：声明式缓存的使用
- **分布式锁**：分布式锁的实现和增强
- **会话管理**：用户会话和token的统一管理
- **架构优化**：会话管理模块的架构重构
- **详细文档**：[模块文档索引](MODULES_INDEX.md#2-synapse-cache-模块)
  - [缓存注解使用指南](modules/synapse-cache/CACHE_ANNOTATIONS_USAGE.md)

### 事件模块 (synapse-events)
- **事件定义**：自定义事件的创建
- **事件发布**：事件的发布机制
- **事件监听**：事件监听器的实现
- **事务事件**：事务相关的事件处理
- **详细文档**：[模块文档索引](MODULES_INDEX.md#5-synapse-events-模块)

## 📋 快速导航

### 🚀 新手上路
1. [README.md](README.md) - 了解框架概览
2. [USAGE_GUIDE.md](USAGE_GUIDE.md) - 快速开始指南
3. [ARCHITECTURE.md](ARCHITECTURE.md) - 深入理解架构
4. [模块文档索引](MODULES_INDEX.md) - 查看各模块详细文档

### 🔧 开发指南
1. **环境搭建** → [USAGE_GUIDE.md#环境准备](USAGE_GUIDE.md#环境准备)
2. **项目创建** → [USAGE_GUIDE.md#项目创建](USAGE_GUIDE.md#项目创建)
3. **实体定义** → [USAGE_GUIDE.md#实体类定义](USAGE_GUIDE.md#实体类定义)
4. **数据访问** → [USAGE_GUIDE.md#repository-层](USAGE_GUIDE.md#repository-层)
5. **业务逻辑** → [USAGE_GUIDE.md#service-层](USAGE_GUIDE.md#service-层)
6. **API 接口** → [USAGE_GUIDE.md#controller-层](USAGE_GUIDE.md#controller-层)

### 🔐 安全配置
1. **认证配置** → [USAGE_GUIDE.md#认证配置](USAGE_GUIDE.md#认证配置)
2. **权限控制** → [USAGE_GUIDE.md#权限控制](USAGE_GUIDE.md#权限控制)
3. **权限管理** → [USAGE_GUIDE.md#权限管理](USAGE_GUIDE.md#权限管理)

### ⚡ 高级功能
1. **缓存使用** → [USAGE_GUIDE.md#缓存功能使用](USAGE_GUIDE.md#缓存功能使用)
2. **事件驱动** → [USAGE_GUIDE.md#事件功能使用](USAGE_GUIDE.md#事件功能使用)
3. **动态数据源** → [USAGE_GUIDE.md#动态数据源](USAGE_GUIDE.md#动态数据源)
4. **批量操作** → [USAGE_GUIDE.md#批量操作](USAGE_GUIDE.md#批量操作)
5. **会话管理** → [DEVELOPMENT_NOTES.md#-会话管理模块重构-2025年](DEVELOPMENT_NOTES.md#-会话管理模块重构-2025年)

## 🎯 使用场景

### 小型项目
- **推荐模块**：`synapse-core` + `synapse-databases`
- **文档参考**：[README.md](README.md) 快速开始部分
- **示例代码**：[USAGE_GUIDE.md](USAGE_GUIDE.md) 基础功能部分

### 中型项目
- **推荐模块**：`synapse-core` + `synapse-databases` + `synapse-security`
- **文档参考**：[USAGE_GUIDE.md](USAGE_GUIDE.md) 安全功能部分
- **最佳实践**：[README.md](README.md) 最佳实践部分

### 大型项目
- **推荐模块**：所有模块
- **文档参考**：[ARCHITECTURE.md](ARCHITECTURE.md) 架构设计部分
- **高级功能**：[USAGE_GUIDE.md](USAGE_GUIDE.md) 高级功能部分

## 🔍 问题解决

### 常见问题
- **编译问题** → [USAGE_GUIDE.md#常见问题](USAGE_GUIDE.md#常见问题)
- **配置问题** → [USAGE_GUIDE.md#基础配置](USAGE_GUIDE.md#基础配置)
- **权限问题** → [USAGE_GUIDE.md#权限控制](USAGE_GUIDE.md#权限控制)
- **缓存问题** → [USAGE_GUIDE.md#缓存功能使用](USAGE_GUIDE.md#缓存功能使用)

### 性能优化
- **数据库优化** → [ARCHITECTURE.md#数据库优化](ARCHITECTURE.md#数据库优化)
- **缓存优化** → [ARCHITECTURE.md#缓存优化](ARCHITECTURE.md#缓存优化)
- **并发优化** → [ARCHITECTURE.md#并发优化](ARCHITECTURE.md#并发优化)

### 安全防护
- **认证安全** → [ARCHITECTURE.md#认证安全](ARCHITECTURE.md#认证安全)
- **授权安全** → [ARCHITECTURE.md#授权安全](ARCHITECTURE.md#授权安全)
- **数据安全** → [ARCHITECTURE.md#数据安全](ARCHITECTURE.md#数据安全)

## 📝 代码示例

### 基础 CRUD
```java
// 实体类
@Data
@TableName("users")
public class User extends AuditEntity<Long> {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String email;
}

// Repository
@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    @QueryCondition
    List<User> findByUsername(String username);
}

// Service
@AutoService
public interface UserService extends BaseRepository<User> {
    User createUser(User user);
    User getUserById(Long id);
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(userService.createUser(user));
    }
}
```

### 权限控制
```java
@RestController
@RequestMapping("/api/admin")
@SaCheckLogin
public class AdminController {
    
    @GetMapping("/users")
    @SaCheckPermission("user:list")
    public Result<PageResult<User>> getUsers(PageDTO pageDTO) {
        return Result.success(userService.getUsersByPage(pageDTO));
    }
}
```

### 缓存使用
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.getById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.updateById(user);
    }
}
```

## 🤝 贡献指南

### 文档贡献
- **完善现有文档**：补充缺失的内容
- **添加使用示例**：提供更多代码示例
- **修复文档错误**：修正文档中的错误

### 代码贡献
- **功能开发**：开发新功能
- **Bug 修复**：修复已知问题
- **性能优化**：优化性能问题

### 问题反馈
- **使用问题**：使用过程中遇到的问题
- **功能建议**：新功能的需求建议
- **文档建议**：文档改进建议

## 📞 联系方式

- **项目地址**：[GitHub Repository]
- **问题反馈**：[GitHub Issues]
- **邮箱联系**：[your-email@example.com]

---

*最后更新时间：2025年1月7日*

**文档版本**：v1.0.0 