# 多表联查功能状态文档

## 📋 概述

本文档记录了 Synapse Framework 数据库模块中多表联查功能的当前状态、现有功能、已知问题和后续改进计划。

**当前状态：⚠️ 暂停使用**

多表联查功能目前处于暂停状态，建议使用 MyBatis-Plus 的方式，在 Mapper 中手写 SQL 进行多表查询。这种方式更稳定、可控，且易于维护。

---

## 🎯 现有功能

### 1. 核心组件

#### 1.1 `@VoMapping` 注解
- **位置**: `synapse-core` 模块
- **功能**: 用于标记 VO 类与数据库表的映射关系，支持多表关联配置
- **状态**: ✅ 可用（但多表查询功能已暂停）

**注解结构**:
```java
@VoMapping(
    table = "iam_resources",      // 主表名
    alias = "res",                // 主表别名（已暂停自动生成）
    joins = {                     // 关联表配置
        @VoMapping.Join(
            table = "iam_menu",
            alias = "menu",
            type = VoMapping.JoinType.LEFT,
            on = "res.menu_id = menu.id"
        )
    },
    fields = {                    // 字段映射
        @VoMapping.Field(source = "res.id", target = "id"),
        @VoMapping.Field(source = "menu.name", target = "menuName")
    }
)
```

#### 1.2 `EnhancedVoFieldSelector`
- **位置**: `synapse-databases/src/main/java/com/indigo/databases/utils/EnhancedVoFieldSelector.java`
- **功能**: 
  - 解析 `@VoMapping` 注解
  - 构建 SELECT 字段列表
  - 构建 JOIN SQL 子句
  - 获取主表名和别名
- **状态**: ✅ 可用（但多表查询功能已暂停）

**主要方法**:
- `getSelectFields(Class<V> voClass)`: 获取 SELECT 字段列表
- `buildJoinSql(Class<V> voClass)`: 构建 JOIN SQL 子句
- `getMainTableName(Class<V> voClass)`: 获取主表名
- `getMainTableAlias(Class<V> voClass)`: 获取主表别名
- `hasJoinQuery(Class<V> voClass)`: 检查是否有 JOIN 查询

#### 1.3 `MultiTableQueryBuilder`
- **位置**: `synapse-databases/src/main/java/com/indigo/databases/utils/MultiTableQueryBuilder.java`
- **功能**: 构建多表查询 SQL
- **状态**: ⚠️ 暂停使用

**主要方法**:
- `buildMultiTableSql(QueryDTO queryDTO, Class<V> voClass)`: 构建完整的多表查询 SQL

#### 1.4 `EnhancedQueryBuilder`
- **位置**: `synapse-databases/src/main/java/com/indigo/databases/utils/EnhancedQueryBuilder.java`
- **功能**: 增强查询构建器，支持单表和多表查询
- **状态**: ⚠️ 多表查询功能已暂停

**相关方法**:
- `pageWithCondition(IService<T> service, PageDTO pageDTO, Class<V> voClass)`: 分页查询（自动判断单表/多表）
- `listWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass)`: 列表查询（自动判断单表/多表）
- `pageWithMultiTableQuery(...)`: 多表分页查询（已暂停）
- `listWithMultiTableQuery(...)`: 多表列表查询（已暂停）

#### 1.5 `BaseRepository` 接口
- **位置**: `synapse-databases/src/main/java/com/indigo/databases/repository/BaseRepository.java`
- **功能**: 提供多表查询的便捷方法
- **状态**: ⚠️ 多表查询方法已暂停

**相关方法**:
- `pageWithVoMapping(PageDTO<?> queryDTO, Class<V> voClass)`: 分页查询并映射到 VO（支持多表）
- `listWithVoMapping(QueryDTO<?> queryDTO, Class<V> voClass)`: 列表查询并映射到 VO（支持多表）
- `getOneWithVoMapping(QueryDTO<?> queryDTO, Class<V> voClass)`: 单条查询并映射到 VO（支持多表）

---

## ⚠️ 已知问题

### 1. 表别名自动生成问题

**问题描述**:
- 当前需要用户手动指定表别名（`@VoMapping.alias` 和 `@VoMapping.Join.alias`）
- 自动生成别名的方案不够成熟，存在以下问题：
  - 表名简化规则不统一（`iam_menu` → `menu`？`iam_user_role` → `ur` 还是 `user_role`？）
  - 同名表冲突处理复杂（`menu`, `menu1`, `menu2`）
  - `source` 和 `on` 中的别名引用需要自动替换，实现复杂

**影响**:
- 用户需要手动维护表别名，容易出错
- 代码不够简洁，维护成本高

### 2. DTO 查询条件问题

**问题描述**:
- 单表 DTO（如 `ResourceDTO`）无法支持多表查询条件
- 当使用多表 VO（如 `ResourceDetailVO`）时，如果需要按关联表字段查询（如按菜单名称查询），DTO 无法提供这些查询条件

**示例**:
```java
// ResourceDTO 只包含资源表字段
public class ResourceDTO extends PageDTO<String> {
    @QueryCondition(field = "code", type = QueryCondition.QueryType.LIKE)
    private String code;  // 资源编码
    
    // 缺少菜单表字段，无法按菜单名称查询
    // private String menuName;
}

// ResourceDetailVO 是多表查询（资源表 + 菜单表 + 系统表）
@VoMapping(...)
public class ResourceDetailVO extends ResourceVO {
    private String menuName;  // 菜单名称
    private String systemName; // 系统名称
}
```

**影响**:
- 无法使用统一的 DTO 进行多表查询
- 需要为多表查询创建专门的 QueryDTO，代码冗余

### 3. WHERE 条件构建问题

**问题描述**:
- `MultiTableQueryBuilder.buildWhereClause()` 方法使用简单的字符串替换来添加表别名
- 无法准确识别字段名，容易误替换
- 不支持复杂的 SQL 表达式

**当前实现**:
```java
// 简单的字符串替换，不够准确
whereSql = whereSql.replaceAll("\\b(account|user_name|...)\\b", tableAlias + ".$1");
```

**影响**:
- SQL 构建可能出错
- 不支持复杂的查询条件

### 4. 参数绑定问题

**问题描述**:
- 多表查询使用 `${sql}` 动态 SQL，需要手动替换参数占位符
- 参数值需要手动转义，存在 SQL 注入风险
- 参数替换逻辑复杂，容易出错

**当前实现**:
```java
// 手动替换参数占位符
String placeholder = "#{ew.paramNameValuePairs." + paramName + "}";
String value = formatSqlValue(paramValue);
result = result.replace(placeholder, value);
```

**影响**:
- 存在 SQL 注入风险
- 参数处理复杂，容易出错

---

## 📝 TODO 列表

### 高优先级

- [ ] **表别名自动生成功能**
  - [ ] 设计统一的表名简化规则（如：去掉 `iam_` 前缀）
  - [ ] 实现同名表冲突处理（`menu`, `menu1`, `menu2`）
  - [ ] 实现 `source` 和 `on` 中的别名自动替换
  - [ ] 提供配置选项，允许用户自定义简化规则
  - [ ] 完善单元测试，覆盖各种边界情况

- [ ] **DTO 查询条件增强**
  - [ ] 支持在 DTO 中指定关联表字段（如 `menu.name`）
  - [ ] 实现字段到表别名的自动映射
  - [ ] 支持字段名冲突处理（使用不同字段名区分）
  - [ ] 提供向后兼容方案

- [ ] **WHERE 条件构建优化**
  - [ ] 使用 SQL 解析器准确识别字段名
  - [ ] 支持复杂的 SQL 表达式
  - [ ] 优化表别名添加逻辑，避免误替换

### 中优先级

- [ ] **参数绑定优化**
  - [ ] 使用 MyBatis 的参数绑定机制，避免手动替换
  - [ ] 实现参数值自动转义，防止 SQL 注入
  - [ ] 优化参数处理逻辑，提升性能

- [ ] **性能优化**
  - [ ] 缓存 `@VoMapping` 注解解析结果
  - [ ] 优化 SQL 构建性能
  - [ ] 提供查询性能监控

- [ ] **错误处理**
  - [ ] 完善错误提示信息
  - [ ] 提供详细的调试日志
  - [ ] 实现错误恢复机制

### 低优先级

- [ ] **文档完善**
  - [ ] 编写多表查询使用指南
  - [ ] 提供最佳实践示例
  - [ ] 更新 API 文档

- [ ] **功能扩展**
  - [ ] 支持子查询
  - [ ] 支持 UNION 查询
  - [ ] 支持 CTE（Common Table Expression）

---

## 🔄 当前推荐方案

### 使用 MyBatis-Plus 手写 SQL

**优点**:
- ✅ 稳定可靠，经过充分验证
- ✅ 灵活可控，可以精确控制 SQL
- ✅ 易于调试和维护
- ✅ 性能可控，可以优化 SQL

**示例**:

```java
// Mapper 接口
@Mapper
public interface ResourceMapper extends BaseMapper<IamResource> {
    
    /**
     * 多表分页查询
     */
    @Select("""
        SELECT 
            res.id,
            res.code,
            res.name,
            menu.name AS menuName,
            sys.name AS systemName
        FROM iam_resources res
        LEFT JOIN iam_menu menu ON res.menu_id = menu.id
        LEFT JOIN iam_system sys ON menu.system_id = sys.id
        WHERE 1=1
        <if test="queryDTO.code != null and queryDTO.code != ''">
            AND res.code LIKE CONCAT('%', #{queryDTO.code}, '%')
        </if>
        <if test="queryDTO.menuName != null and queryDTO.menuName != ''">
            AND menu.name LIKE CONCAT('%', #{queryDTO.menuName}, '%')
        </if>
        ORDER BY res.create_time DESC
    """)
    IPage<ResourceDetailVO> selectResourceDetailPage(IPage<ResourceDetailVO> page, @Param("queryDTO") ResourceDetailQueryDTO queryDTO);
}

// Repository 接口
@AutoRepository
public interface IResourceService extends BaseRepository<IamResource, ResourceMapper> {
    
    /**
     * 多表分页查询
     */
    default PageResult<ResourceDetailVO> pageResourceDetail(ResourceDetailQueryDTO queryDTO) {
        Page<ResourceDetailVO> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        IPage<ResourceDetailVO> result = getMapper().selectResourceDetailPage(page, queryDTO);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}
```

---

## 📚 相关文件

### 核心文件
- `synapse-core/src/main/java/com/indigo/core/annotation/VoMapping.java` - VO 映射注解
- `synapse-databases/src/main/java/com/indigo/databases/utils/EnhancedVoFieldSelector.java` - VO 字段选择器
- `synapse-databases/src/main/java/com/indigo/databases/utils/MultiTableQueryBuilder.java` - 多表查询构建器
- `synapse-databases/src/main/java/com/indigo/databases/utils/EnhancedQueryBuilder.java` - 增强查询构建器
- `synapse-databases/src/main/java/com/indigo/databases/repository/BaseRepository.java` - 基础 Repository 接口

### 使用示例
- `foundation-module/iam-service/iam-sdk/src/main/java/com/indigo/iam/sdk/vo/resource/ResourceDetailVO.java`
- `foundation-module/iam-service/iam-sdk/src/main/java/com/indigo/iam/sdk/vo/users/UserResourceVO.java`
- `foundation-module/iam-service/iam-sdk/src/main/java/com/indigo/iam/sdk/vo/users/UserRoleVO.java`

---

## 📅 更新记录

- **2025-01-XX**: 创建文档，记录多表联查功能状态
- **2025-01-XX**: 暂停多表联查功能，推荐使用 MyBatis-Plus 手写 SQL

---

## 🤝 贡献

如有改进建议或发现问题，请提交 Issue 或 Pull Request。

