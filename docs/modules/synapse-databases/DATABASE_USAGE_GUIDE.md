# Synapse Framework 数据库模块使用指南 🚀

> 这不是一个枯燥的API文档，而是一个真正能帮你写出优雅代码的实用指南！

## 目录
- [快速开始](#快速开始)
- [单表操作](#单表操作)
- [多表关联查询](#多表关联查询)
- [高级查询技巧](#高级查询技巧)
- [实际项目示例](#实际项目示例)
- [性能优化](#性能优化)
- [常见问题](#常见问题)

---

## 快速开始

### 为什么选择 Synapse Framework？

想象一下，你不需要写这样的代码：
```java
// 传统方式 - 繁琐且容易出错
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("username", username);
wrapper.eq("status", 1);
wrapper.ge("create_time", startTime);
wrapper.le("create_time", endTime);
wrapper.orderByDesc("create_time");
List<User> users = userService.list(wrapper);
```

而是这样：
```java
// Synapse 方式 - 简洁优雅
UserQueryDTO query = new UserQueryDTO();
query.setUsername(username);
query.setStatus(1);
query.setStartTime(startTime);
query.setEndTime(endTime);
query.setOrderByList(Arrays.asList(new OrderBy("create_time", "DESC")));

List<User> users = userRepository.listWithDTO(query);
```

**这就是 Synapse 的魅力！** 🎯

---

## 单表操作

### 1. 基础 CRUD - 开箱即用

```java
// 你的 Repository 接口
public interface UserRepository extends BaseRepository<User, UserMapper> {
    // 什么都不用写！所有基础功能都有了
}

// 使用示例
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 插入 - 自动填充审计字段
    public void createUser(User user) {
        userRepository.save(user); // 自动设置 createTime, updateTime, createBy 等
    }
    
    // 批量插入 - 性能优化
    public void batchCreateUsers(List<User> users) {
        userRepository.saveBatch(users, 1000); // 分批插入，避免内存溢出
    }
    
    // 更新 - 自动填充 updateTime
    public void updateUser(User user) {
        userRepository.updateById(user); // 自动设置 updateTime
    }
    
    // 删除 - 逻辑删除
    public void deleteUser(Long id) {
        userRepository.removeById(id); // 自动设置 deleted = 1
    }
}
```

### 2. 智能查询 - 告别 QueryWrapper

```java
// 传统方式 vs Synapse 方式
public class UserService {
    
    // ❌ 传统方式 - 手写 QueryWrapper
    public List<User> findUsersOld(String username, Integer status, Date startTime) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(username)) {
            wrapper.like("username", username);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (startTime != null) {
            wrapper.ge("create_time", startTime);
        }
        return userRepository.list(wrapper);
    }
    
    // ✅ Synapse 方式 - 自动构建查询条件
    public List<User> findUsersNew(String username, Integer status, Date startTime) {
        UserQueryDTO query = new UserQueryDTO();
        query.setUsername(username);
        query.setStatus(status);
        query.setStartTime(startTime);
        
        return userRepository.listWithDTO(query); // 自动构建 QueryWrapper！
    }
}
```

### 3. 分页查询 - 一行代码搞定

```java
public class UserService {
    
    // 分页查询 - 自动处理分页逻辑
    public PageResult<User> pageUsers(Integer pageNo, Integer pageSize, String username) {
        UserQueryDTO query = new UserQueryDTO();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setUsername(username);
        
        // 一行代码搞定分页！
        return userRepository.pageWithCondition(query);
    }
    
    // 带排序的分页查询
    public PageResult<User> pageUsersWithOrder(Integer pageNo, Integer pageSize) {
        UserQueryDTO query = new UserQueryDTO();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        
        // 支持多字段排序
        query.setOrderByList(Arrays.asList(
            new OrderBy("status", "ASC"),      // 先按状态升序
            new OrderBy("create_time", "DESC") // 再按创建时间降序
        ));
        
        return userRepository.pageWithCondition(query);
    }
}
```

---

## 多表关联查询

### 1. 使用框架提供的多表关联

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 使用框架的 JoinPageDTO - 简单场景
    default PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        // 配置多表关联
        queryDTO.setTableJoins(Arrays.asList(
            new TableJoin("sys_department", "d", JoinType.LEFT, "u.dept_id = d.id"),
            new TableJoin("sys_role", "r", JoinType.LEFT, "u.role_id = r.id"),
            new TableJoin("sys_user_profile", "p", JoinType.LEFT, "u.id = p.user_id")
        ));
        
        // 设置选择字段
        queryDTO.getTableJoins().get(0).setSelectFields(Arrays.asList("d.dept_name", "d.dept_code"));
        queryDTO.getTableJoins().get(1).setSelectFields(Arrays.asList("r.role_name", "r.role_code"));
        queryDTO.getTableJoins().get(2).setSelectFields(Arrays.asList("p.real_name", "p.avatar"));
        
        // 执行查询
        PageResult<User> pageResult = pageWithJoin(queryDTO);
        
        // 转换为结果DTO
        List<UserJoinResultDTO> resultList = pageResult.getRecords().stream()
            .map(this::convertToJoinResult)
            .collect(Collectors.toList());
        
        return new PageResult<>(resultList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }
}
```

### 2. 手写 SQL - 复杂场景的终极武器

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 复杂多表关联查询 - 手写 SQL
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code, d.parent_id as dept_parent_id,
            r.role_name, r.role_code, r.description as role_description,
            p.real_name, p.avatar, p.address, p.birthday,
            -- 统计信息
            COUNT(ul.id) as login_count,
            MAX(ul.login_time) as last_login_time,
            COUNT(ua.id) as action_count
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        LEFT JOIN sys_user_profile p ON u.id = p.user_id
        LEFT JOIN sys_user_login ul ON u.id = ul.user_id 
            AND ul.login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        LEFT JOIN sys_user_action ua ON u.id = ua.user_id 
            AND ua.action_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        WHERE u.deleted = 0 
            AND u.status = #{status}
            AND (#{username} IS NULL OR u.username LIKE CONCAT('%', #{username}, '%'))
            AND (#{deptName} IS NULL OR d.dept_name LIKE CONCAT('%', #{deptName}, '%'))
        GROUP BY u.id, u.username, u.email, u.phone, u.status,
                 u.dept_id, u.role_id, u.create_time, u.update_time,
                 d.dept_name, d.dept_code, d.parent_id,
                 r.role_name, r.role_code, r.description,
                 p.real_name, p.avatar, p.address, p.birthday
        HAVING login_count > 0 OR action_count > 0
        ORDER BY u.create_time DESC
        """)
    List<UserJoinResultDTO> selectUsersWithComplexJoin(@Param("username") String username, 
                                                       @Param("status") Integer status,
                                                       @Param("deptName") String deptName);
    
    // 递归查询 - 部门层级结构
    @Select("""
        WITH RECURSIVE dept_tree AS (
            -- 获取根部门
            SELECT id, dept_name, dept_code, parent_id, 0 as level, sort
            FROM sys_department 
            WHERE parent_id IS NULL AND deleted = 0
            
            UNION ALL
            
            -- 递归查询子部门
            SELECT d.id, d.dept_name, d.dept_code, d.parent_id, dt.level + 1, d.sort
            FROM sys_department d
            INNER JOIN dept_tree dt ON d.parent_id = dt.id
            WHERE d.deleted = 0
        )
        SELECT 
            dt.*,
            COUNT(u.id) as user_count,
            SUM(CASE WHEN u.status = 1 THEN 1 ELSE 0 END) as active_user_count
        FROM dept_tree dt
        LEFT JOIN sys_user u ON dt.id = u.dept_id AND u.deleted = 0
        GROUP BY dt.id, dt.dept_name, dt.dept_code, dt.parent_id, dt.level, dt.sort
        ORDER BY dt.level, dt.sort
        """)
    List<Map<String, Object>> selectDeptHierarchyWithUserCount();
}
```

### 3. 动态 SQL - 灵活的条件构建

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 动态 SQL - 根据条件动态构建查询
    @Select("""
        <script>
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        <where>
            u.deleted = 0 AND d.deleted = 0 AND r.deleted = 0
            
            <if test="username != null and username != ''">
                AND u.username LIKE CONCAT('%', #{username}, '%')
            </if>
            
            <if test="email != null and email != ''">
                AND u.email LIKE CONCAT('%', #{email}, '%')
            </if>
            
            <if test="status != null">
                AND u.status = #{status}
            </if>
            
            <if test="deptIds != null and deptIds.size() > 0">
                AND u.dept_id IN
                <foreach collection="deptIds" item="deptId" open="(" separator="," close=")">
                    #{deptId}
                </foreach>
            </if>
            
            <if test="roleIds != null and roleIds.size() > 0">
                AND u.role_id IN
                <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                    #{roleId}
                </foreach>
            </if>
            
            <if test="startTime != null">
                AND u.create_time >= #{startTime}
            </if>
            
            <if test="endTime != null">
                AND u.create_time <= #{endTime}
            </if>
        </where>
        
        <choose>
            <when test="orderByList != null and orderByList.size() > 0">
                ORDER BY
                <foreach collection="orderByList" item="orderBy" separator=",">
                    ${orderBy.field} ${orderBy.direction}
                </foreach>
            </when>
            <otherwise>
                ORDER BY u.create_time DESC
            </otherwise>
        </choose>
        </script>
        """)
    List<UserJoinResultDTO> selectUsersWithDynamicCondition(UserQueryDTO queryDTO);
}
```

---

## 高级查询技巧

### 1. 聚合查询 - 数据分析的利器

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 用户行为分析 - 多维度统计
    @Select("""
        SELECT 
            d.dept_name,
            r.role_name,
            COUNT(u.id) as total_users,
            SUM(CASE WHEN u.status = 1 THEN 1 ELSE 0 END) as active_users,
            SUM(CASE WHEN u.status = 0 THEN 1 ELSE 0 END) as inactive_users,
            ROUND(AVG(CASE WHEN u.status = 1 THEN 1 ELSE 0 END) * 100, 2) as active_rate,
            SUM(CASE WHEN u.create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as new_users_30d,
            MAX(u.create_time) as latest_user_time,
            MIN(u.create_time) as earliest_user_time,
            -- 登录统计
            SUM(ul.login_count) as total_logins,
            ROUND(AVG(ul.login_count), 2) as avg_logins_per_user,
            -- 行为统计
            SUM(ua.action_count) as total_actions,
            COUNT(DISTINCT ua.action_type) as unique_action_types
        FROM sys_department d
        LEFT JOIN sys_user u ON d.id = u.dept_id AND u.deleted = 0
        LEFT JOIN sys_role r ON u.role_id = r.id AND r.deleted = 0
        LEFT JOIN (
            SELECT user_id, COUNT(*) as login_count
            FROM sys_user_login 
            WHERE login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            GROUP BY user_id
        ) ul ON u.id = ul.user_id
        LEFT JOIN (
            SELECT user_id, COUNT(*) as action_count, action_type
            FROM sys_user_action 
            WHERE action_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            GROUP BY user_id, action_type
        ) ua ON u.id = ua.user_id
        WHERE d.deleted = 0
        GROUP BY d.id, d.dept_name, r.id, r.role_name
        HAVING total_users > 0
        ORDER BY total_users DESC, active_rate DESC
        """)
    List<Map<String, Object>> selectUserBehaviorAnalysis();
}
```

### 2. 性能优化查询 - 让数据库飞起来

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 分页查询优化 - 使用游标分页避免深度分页问题
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.deleted = 0 
            AND u.id > #{lastId}  -- 游标分页，避免 OFFSET
            AND u.create_time >= #{startTime}
        ORDER BY u.id ASC
        LIMIT #{pageSize}
        """)
    List<UserJoinResultDTO> selectUsersWithCursorPagination(@Param("lastId") Long lastId,
                                                           @Param("pageSize") Integer pageSize,
                                                           @Param("startTime") Date startTime);
    
    // 字段选择优化 - 只查询需要的字段
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.status,  -- 只选择必要字段
            d.dept_name, r.role_name               -- 关联表只选择显示字段
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.deleted = 0 
            AND u.status = #{status}
        ORDER BY u.create_time DESC
        LIMIT #{limit}
        """)
    List<UserJoinResultDTO> selectUsersOptimized(@Param("status") Integer status, 
                                                @Param("limit") Integer limit);
}
```

---

## 实际项目示例

### 电商系统用户管理

```java
// 用户查询 DTO
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageDTO {
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private String deptName;
    private String roleName;
    private Date startTime;
    private Date endTime;
    private List<Long> deptIds;
    private List<Long> roleIds;
    private String realName;
    private String address;
    
    // 业务字段
    private Integer minOrderCount;      // 最小订单数
    private Integer maxOrderCount;      // 最大订单数
    private BigDecimal minTotalAmount;  // 最小消费金额
    private BigDecimal maxTotalAmount;  // 最大消费金额
    private String lastLoginIp;         // 最后登录IP
    private String userLevel;           // 用户等级
}

// 用户 Repository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 电商用户综合查询 - 包含订单、消费、行为等数据
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code,
            p.real_name, p.avatar, p.address, p.birthday,
            -- 订单统计
            COUNT(o.id) as order_count,
            SUM(o.total_amount) as total_amount,
            MAX(o.create_time) as last_order_time,
            -- 登录统计
            COUNT(ul.id) as login_count,
            MAX(ul.login_time) as last_login_time,
            ul.ip_address as last_login_ip,
            -- 行为统计
            COUNT(ua.id) as action_count,
            COUNT(DISTINCT ua.action_type) as action_type_count,
            -- 用户等级
            CASE 
                WHEN SUM(o.total_amount) >= 10000 THEN 'VIP'
                WHEN SUM(o.total_amount) >= 5000 THEN 'GOLD'
                WHEN SUM(o.total_amount) >= 1000 THEN 'SILVER'
                ELSE 'BRONZE'
            END as user_level
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        LEFT JOIN sys_user_profile p ON u.id = p.user_id
        LEFT JOIN sys_order o ON u.id = o.user_id AND o.status = 'COMPLETED'
        LEFT JOIN sys_user_login ul ON u.id = ul.user_id
        LEFT JOIN sys_user_action ua ON u.id = ua.user_id
        WHERE u.deleted = 0 
            AND u.status = #{status}
            AND (#{username} IS NULL OR u.username LIKE CONCAT('%', #{username}, '%'))
            AND (#{email} IS NULL OR u.email LIKE CONCAT('%', #{email}, '%'))
            AND (#{phone} IS NULL OR u.phone LIKE CONCAT('%', #{phone}, '%'))
            AND (#{deptName} IS NULL OR d.dept_name LIKE CONCAT('%', #{deptName}, '%'))
            AND (#{roleName} IS NULL OR r.role_name LIKE CONCAT('%', #{roleName}, '%'))
            AND (#{realName} IS NULL OR p.real_name LIKE CONCAT('%', #{realName}, '%'))
            AND (#{address} IS NULL OR p.address LIKE CONCAT('%', #{address}, '%'))
            AND (#{startTime} IS NULL OR u.create_time >= #{startTime})
            AND (#{endTime} IS NULL OR u.create_time <= #{endTime})
            AND (#{minOrderCount} IS NULL OR COUNT(o.id) >= #{minOrderCount})
            AND (#{maxOrderCount} IS NULL OR COUNT(o.id) <= #{maxOrderCount})
            AND (#{minTotalAmount} IS NULL OR SUM(o.total_amount) >= #{minTotalAmount})
            AND (#{maxTotalAmount} IS NULL OR SUM(o.total_amount) <= #{maxTotalAmount})
            AND (#{lastLoginIp} IS NULL OR ul.ip_address = #{lastLoginIp})
        GROUP BY u.id, u.username, u.email, u.phone, u.status,
                 u.dept_id, u.role_id, u.create_time, u.update_time,
                 d.dept_name, d.dept_code, r.role_name, r.role_code,
                 p.real_name, p.avatar, p.address, p.birthday,
                 ul.ip_address
        HAVING (#{userLevel} IS NULL OR user_level = #{userLevel})
        ORDER BY u.create_time DESC
        """)
    List<UserJoinResultDTO> selectEcommerceUsers(UserQueryDTO queryDTO);
}
```

---

## 性能优化

### 1. 查询优化技巧

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 索引优化 - 确保查询字段有索引
    @Select("""
        -- 建议在以下字段上创建索引：
        -- CREATE INDEX idx_user_status_create_time ON sys_user(status, create_time);
        -- CREATE INDEX idx_user_dept_id ON sys_user(dept_id);
        -- CREATE INDEX idx_user_role_id ON sys_user(role_id);
        -- CREATE INDEX idx_user_username ON sys_user(username);
        -- CREATE INDEX idx_user_email ON sys_user(email);
        
        SELECT 
            u.id, u.username, u.email, u.status, u.create_time,
            d.dept_name, r.role_name
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.deleted = 0 
            AND u.status = #{status}           -- 使用复合索引 (status, create_time)
            AND u.create_time >= #{startTime}  -- 范围查询
        ORDER BY u.create_time DESC            -- 避免文件排序
        LIMIT #{pageSize}
        """)
    List<UserJoinResultDTO> selectUsersOptimized(@Param("status") Integer status,
                                                @Param("startTime") Date startTime,
                                                @Param("pageSize") Integer pageSize);
}
```

### 2. 缓存策略

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 使用 Redis 缓存热点数据
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.id = #{userId}
        """)
    UserJoinResultDTO selectUserById(@Param("userId") Long userId);
    
    // 批量查询优化 - 使用 IN 查询避免 N+1 问题
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.deleted = 0 
            AND u.id IN
        <foreach collection="userIds" item="userId" open="(" separator="," close=")">
            #{userId}
        </foreach>
        ORDER BY FIELD(u.id, 
        <foreach collection="userIds" item="userId" separator=",">
            #{userId}
        </foreach>
        )
        """)
    List<UserJoinResultDTO> selectUsersByIds(@Param("userIds") List<Long> userIds);
    
    // 分页查询优化 - 使用游标分页避免深度分页问题
    @Select("""
        SELECT 
            u.id, u.username, u.email, u.phone, u.status,
            u.dept_id, u.role_id, u.create_time, u.update_time,
            d.dept_name, d.dept_code,
            r.role_name, r.role_code
        FROM sys_user u
        LEFT JOIN sys_department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON u.role_id = r.id
        WHERE u.deleted = 0 
            AND u.id > #{lastId}  -- 游标分页，避免 OFFSET
            AND u.create_time >= #{startTime}
        ORDER BY u.id ASC
        LIMIT #{pageSize}
        """)
    List<UserJoinResultDTO> selectUsersWithCursorPagination(@Param("lastId") Long lastId,
                                                           @Param("pageSize") Integer pageSize,
                                                           @Param("startTime") Date startTime);
}

---

## 高级特性

### 1. 动态数据源切换

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 从主库查询
    @DS("master")
    default List<User> getUsersFromMaster() {
        return list();
    }
    
    // 从从库查询
    @DS("slave")
    default List<User> getUsersFromSlave() {
        return list();
    }
    
    // 编程式切换数据源
    default List<User> getUsersFromSpecificDataSource(String dataSourceName) {
        DynamicDataSourceContextHolder.setDataSource(dataSourceName);
        try {
            return list();
        } finally {
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
}
```

### 2. 事务管理

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 在 Repository 层管理事务
    @Transactional(rollbackFor = Exception.class)
    default void createUserWithProfile(User user, UserProfile profile) {
        // 保存用户
        save(user);
        
        // 保存用户档案
        profile.setUserId(user.getId());
        // 这里需要注入 UserProfileRepository 或者通过 Service 层调用
        
        // 如果任何一步失败，整个事务回滚
    }
    
    // 只读事务优化
    @Transactional(readOnly = true)
    default List<User> getUsersReadOnly() {
        return list();
    }
}
```

### 3. 审计功能

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 自动审计字段填充
    default void createUserWithAudit(User user) {
        // 继承 BaseEntity 的实体会自动填充：
        // - createTime: 创建时间
        // - updateTime: 更新时间  
        // - createBy: 创建人（从 UserContext 获取）
        // - updateBy: 更新人（从 UserContext 获取）
        // - deleted: 逻辑删除标记
        
        save(user);
    }
    
    // 查询审计日志
    @Select("""
        SELECT 
            u.id, u.username, u.create_time, u.create_by,
            u.update_time, u.update_by, u.deleted
        FROM sys_user u
        WHERE u.id = #{userId}
        """)
    Map<String, Object> getUserAuditInfo(@Param("userId") Long userId);
}
```

---

## 实际应用场景

### 1. 权限管理系统

```java
public interface PermissionRepository extends BaseRepository<Permission, PermissionMapper> {
    
    // 获取用户权限树
    @Select("""
        WITH RECURSIVE permission_tree AS (
            SELECT 
                p.id, p.permission_name, p.permission_code, p.permission_type,
                p.parent_id, p.permission_url, p.permission_method,
                0 as level, p.sort, p.status
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user u ON rp.role_id = u.role_id
            WHERE u.id = #{userId} AND u.deleted = 0 AND p.deleted = 0
            
            UNION ALL
            
            SELECT 
                cp.id, cp.permission_name, cp.permission_code, cp.permission_type,
                cp.parent_id, cp.permission_url, cp.permission_method,
                pt.level + 1, cp.sort, cp.status
            FROM sys_permission cp
            INNER JOIN permission_tree pt ON cp.parent_id = pt.id
            WHERE cp.deleted = 0
        )
        SELECT 
            pt.*,
            CASE 
                WHEN pt.level = 0 THEN 'ROOT'
                WHEN pt.parent_id IS NULL THEN 'ORPHAN'
                ELSE 'CHILD'
            END as node_type
        FROM permission_tree pt
        ORDER BY pt.level, pt.sort
        """)
    List<Map<String, Object>> getUserPermissionTree(@Param("userId") Long userId);
    
    // 检查用户是否有特定权限
    @Select("""
        SELECT COUNT(*) > 0 as has_permission
        FROM sys_user u
        INNER JOIN sys_role_permission rp ON u.role_id = rp.role_id
        INNER JOIN sys_permission p ON rp.permission_id = p.id
        WHERE u.id = #{userId} 
            AND p.permission_code = #{permissionCode}
            AND u.deleted = 0 AND p.deleted = 0
        """)
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionCode") String permissionCode);
}
```

### 2. 工作流系统

```java
public interface WorkflowRepository extends BaseRepository<Workflow, WorkflowMapper> {
    
    // 获取用户待办任务
    @Select("""
        SELECT 
            t.id, t.task_name, t.task_type, t.priority, t.create_time,
            w.workflow_name, w.workflow_type,
            u.username as assignee_name,
            d.dept_name as assignee_dept
        FROM sys_workflow_task t
        INNER JOIN sys_workflow w ON t.workflow_id = w.id
        INNER JOIN sys_user u ON t.assignee_id = u.id
        LEFT JOIN sys_department d ON u.dept_id = d.id
        WHERE t.assignee_id = #{userId}
            AND t.status = 'PENDING'
            AND t.deleted = 0
        ORDER BY t.priority DESC, t.create_time ASC
        """)
    List<Map<String, Object>> getUserPendingTasks(@Param("userId") Long userId);
    
    // 获取工作流统计信息
    @Select("""
        SELECT 
            w.workflow_type,
            COUNT(t.id) as total_tasks,
            SUM(CASE WHEN t.status = 'PENDING' THEN 1 ELSE 0 END) as pending_tasks,
            SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
            AVG(TIMESTAMPDIFF(HOUR, t.create_time, t.complete_time)) as avg_completion_hours
        FROM sys_workflow w
        LEFT JOIN sys_workflow_task t ON w.id = t.workflow_id
        WHERE w.deleted = 0 AND (t.deleted = 0 OR t.deleted IS NULL)
        GROUP BY w.workflow_type
        ORDER BY total_tasks DESC
        """)
    List<Map<String, Object>> getWorkflowStatistics();
}
```

---

## 最佳实践

### 1. 命名规范

```java
// ✅ 好的命名
public interface UserRepository extends BaseRepository<User, UserMapper> {
    // 查询方法以 select 开头
    List<User> selectActiveUsers();
    
    // 统计方法以 count 开头
    long countUsersByStatus(Integer status);
    
    // 检查方法以 has 或 exists 开头
    boolean hasUserWithEmail(String email);
    
    // 更新方法以 update 开头
    int updateUserStatus(Long userId, Integer status);
}

// ❌ 不好的命名
public interface UserRepository extends BaseRepository<User, UserMapper> {
    List<User> getActiveUsers();        // 应该用 select
    long getCountByStatus(Integer status); // 应该用 count
    boolean checkEmail(String email);   // 应该用 has 或 exists
}
```

### 2. 查询优化原则

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // ✅ 好的做法
    @Select("""
        SELECT u.id, u.username, u.email  -- 只选择需要的字段
        FROM sys_user u
        WHERE u.deleted = 0               -- 使用索引字段
            AND u.status = #{status}      -- 使用索引字段
        ORDER BY u.create_time DESC       -- 使用索引排序
        LIMIT #{pageSize}                 -- 限制结果集大小
        """)
    List<User> selectUsersOptimized(@Param("status") Integer status, 
                                   @Param("pageSize") Integer pageSize);
    
    // ❌ 不好的做法
    @Select("""
        SELECT *                          -- 选择所有字段
        FROM sys_user u
        WHERE u.username LIKE '%admin%'   -- 不使用索引的模糊查询
        ORDER BY u.email                  -- 不使用索引排序
        """)
    List<User> selectUsersBad();
}
```

### 3. 异常处理

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // ✅ 好的异常处理
    default User getUserByIdSafely(Long userId) {
        try {
            return getById(userId);
        } catch (Exception e) {
            log.error("查询用户失败，用户ID: {}", userId, e);
            throw new BusinessException("查询用户失败: " + e.getMessage());
        }
    }
    
    // ✅ 使用 Optional 处理空值
    default Optional<User> getUserByIdOptional(Long userId) {
        try {
            User user = getById(userId);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("查询用户失败，用户ID: {}", userId, e);
            return Optional.empty();
        }
    }
}
```

---

## 常见问题与解决方案

### 1. N+1 查询问题

```java
// ❌ 问题代码 - 会产生 N+1 查询
public List<UserDetailDTO> getUsersWithDetails(List<Long> userIds) {
    List<UserDetailDTO> result = new ArrayList<>();
    for (Long userId : userIds) {
        User user = getById(userId);                    // 1次查询
        Department dept = getDeptById(user.getDeptId()); // N次查询
        Role role = getRoleById(user.getRoleId());       // N次查询
        result.add(new UserDetailDTO(user, dept, role));
    }
    return result;
}

// ✅ 解决方案 - 使用 JOIN 查询
@Select("""
    SELECT 
        u.id, u.username, u.email, u.phone, u.status,
        d.dept_name, d.dept_code,
        r.role_name, r.role_code
    FROM sys_user u
    LEFT JOIN sys_department d ON u.dept_id = d.id
    LEFT JOIN sys_role r ON u.role_id = r.id
    WHERE u.id IN
    <foreach collection="userIds" item="userId" open="(" separator="," close=")">
        #{userId}
    </foreach>
    """)
List<UserDetailDTO> selectUsersWithDetails(@Param("userIds") List<Long> userIds);
```

### 2. 深度分页问题

```java
// ❌ 问题代码 - 深度分页性能差
@Select("""
    SELECT * FROM sys_user 
    ORDER BY create_time DESC 
    LIMIT #{offset}, #{pageSize}
    """)
List<User> selectUsersWithOffset(@Param("offset") Integer offset, 
                                 @Param("pageSize") Integer pageSize);

// ✅ 解决方案 - 使用游标分页
@Select("""
    SELECT * FROM sys_user 
    WHERE id > #{lastId}  -- 使用 ID 作为游标
    ORDER BY id ASC        -- 按 ID 排序
    LIMIT #{pageSize}
    """)
List<User> selectUsersWithCursor(@Param("lastId") Long lastId, 
                                 @Param("pageSize") Integer pageSize);
```

### 3. 大数据量处理

```java
// ❌ 问题代码 - 一次性加载所有数据
@Select("SELECT * FROM sys_user")
List<User> selectAllUsers();

// ✅ 解决方案 - 分批处理
default void processAllUsersInBatches(Consumer<List<User>> processor) {
    int batchSize = 1000;
    long offset = 0;
    
    while (true) {
        List<User> batch = selectUsersBatch(offset, batchSize);
        if (batch.isEmpty()) {
            break;
        }
        
        processor.accept(batch);
        offset += batchSize;
    }
}

@Select("""
    SELECT * FROM sys_user 
    ORDER BY id 
    LIMIT #{offset}, #{batchSize}
    """)
List<User> selectUsersBatch(@Param("offset") Long offset, 
                            @Param("batchSize") Integer batchSize);
```

---

## 总结

Synapse Framework 数据库模块为你提供了：

🎯 **开箱即用的基础功能** - 继承 `BaseRepository` 就能获得所有 MyBatis-Plus 功能
🚀 **智能查询构建** - 告别手写 QueryWrapper，让代码更优雅
🔗 **灵活的多表关联** - 支持框架方法和手写 SQL 两种方式
⚡ **性能优化工具** - 内置分页、缓存、索引优化等最佳实践
🛡️ **企业级特性** - 审计、事务、动态数据源等生产环境必需功能

记住：**好的代码不是写出来的，而是设计出来的**。Synapse Framework 帮你专注于业务逻辑，而不是重复的 CRUD 代码。

现在，去写那些真正有价值的业务代码吧！🚀