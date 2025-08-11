# Synapse Framework 模块文档索引

## 概述

本文档索引了 Synapse Framework 各个模块的详细文档，方便开发者快速找到所需信息。

## 模块文档

### 1. Synapse Security 模块
**位置**: `docs/modules/synapse-security/`

**文档列表**:
- [使用示例](modules/synapse-security/USAGE_EXAMPLES.md) - 详细的使用示例和最佳实践
- [重构总结](modules/synapse-security/REFACTORING_SUMMARY.md) - 模块重构的详细说明
- [Sa-Token 使用指南](modules/synapse-security/SA_TOKEN_USAGE.md) - Sa-Token 框架的使用说明
- [优化总结](modules/synapse-security/OPTIMIZATION_SUMMARY.md) - 性能优化和架构优化总结
- [JWT 配置示例](modules/synapse-security/jwt-example.yml) - JWT 配置的 YAML 示例

**模块简介**: 
Synapse Security 模块提供了完整的认证、授权和权限管理功能，支持多种认证策略（Sa-Token、OAuth2.0、JWT等）。

### 2. Synapse Cache 模块
**位置**: `docs/modules/synapse-cache/`

**文档列表**:
- [README](modules/synapse-cache/README.md) - 模块概述和快速开始指南
- [缓存注解使用指南](modules/synapse-cache/CACHE_ANNOTATIONS_USAGE.md) - 缓存注解的详细使用说明
- [分布式锁优化文档](modules/synapse-cache/DISTRIBUTED_LOCK_OPTIMIZATION.md) - 分布式锁功能详解和最佳实践
- [优化工作总结](modules/synapse-cache/OPTIMIZATION_SUMMARY.md) - 模块优化历程和成果总结

**模块简介**: 
Synapse Cache 模块提供了强大的缓存管理功能，支持多种缓存策略、注解驱动的缓存操作和分布式锁服务。采用延迟初始化和自动释放机制，确保资源的高效利用。

### 3. Synapse Databases 模块
**位置**: `docs/modules/synapse-databases/`

**文档列表**: 
- 暂无独立文档（功能说明请参考主文档）

**模块简介**: 
Synapse Databases 模块提供了数据库连接管理、智能数据源路由、自动读写分离等功能。支持多种数据库类型和连接池，无需注解即可实现数据源自动切换。

### 4. Synapse Core 模块
**位置**: `docs/modules/synapse-core/`

**文档列表**: 
- 暂无独立文档（功能说明请参考主文档）

**模块简介**: 
Synapse Core 模块是框架的核心，提供了基础配置、上下文管理、国际化等核心功能。

### 5. Synapse Events 模块
**位置**: `docs/modules/synapse-events/`

**文档列表**: 
- 暂无独立文档（功能说明请参考主文档）

**模块简介**: 
Synapse Events 模块提供了事件驱动架构支持，包括事件发布、订阅、事务事件等功能。

## 文档导航

### 按功能分类
- **认证授权**: [Security 模块文档](modules/synapse-security/)
- **缓存管理**: [Cache 模块文档](modules/synapse-cache/)
- **数据库操作**: [Databases 模块文档](modules/synapse-databases/)
- **核心功能**: [Core 模块文档](modules/synapse-core/)
- **事件处理**: [Events 模块文档](modules/synapse-events/)

### 按文档类型分类
- **使用指南**: USAGE_EXAMPLES.md, CACHE_ANNOTATIONS_USAGE.md
- **架构说明**: REFACTORING_SUMMARY.md, OPTIMIZATION_SUMMARY.md
- **配置示例**: jwt-example.yml
- **技术文档**: SA_TOKEN_USAGE.md

## 快速开始

### 新用户推荐阅读顺序
1. [主文档](README.md) - 了解框架整体架构
2. [使用指南](USAGE_GUIDE.md) - 学习基本使用方法
3. [Security 模块使用示例](modules/synapse-security/USAGE_EXAMPLES.md) - 学习认证授权
4. [Cache 模块 README](modules/synapse-cache/README.md) - 了解缓存模块功能
5. [Cache 模块使用指南](modules/synapse-cache/CACHE_ANNOTATIONS_USAGE.md) - 学习缓存管理

### 开发者推荐阅读顺序
1. [架构文档](ARCHITECTURE.md) - 深入理解架构设计
2. [开发笔记](DEVELOPMENT_NOTES.md) - 了解开发过程中的重要决策
3. [重构总结](modules/synapse-security/REFACTORING_SUMMARY.md) - 了解架构演进
4. [优化总结](modules/synapse-security/OPTIMIZATION_SUMMARY.md) - 学习性能优化

## 文档维护

### 文档更新原则
- 模块功能变更时，及时更新对应文档
- 新增功能时，同步创建使用说明文档
- 重构完成后，更新架构和重构说明文档

### 文档贡献
欢迎开发者贡献文档，包括：
- 使用示例和最佳实践
- 常见问题解答
- 性能优化建议
- 架构设计思路

## 联系方式

如有文档相关问题或建议，请联系：
- 项目维护者：史偕成
- 项目地址：[GitHub Repository]
- 问题反馈：[Issues]

---

*最后更新: 2025年08月11日 12:41:56* 