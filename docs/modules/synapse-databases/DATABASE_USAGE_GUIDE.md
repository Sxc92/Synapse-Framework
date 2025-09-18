# Synapse Framework 数据库模块使用指�?🚀

> 这不是一个枯燥的API文档，而是一个真正能帮你写出优雅代码的实用指南！

## 目录
- [快速开始](#快速开�?
- [基础配置](#基础配置)
- [实体类设计](#实体类设�?
- [Repository层开发](#repository层开�?
- [查询条件构建](#查询条件构建)
- [分页查询](#分页查询)
- [多表关联查询](#多表关联查询)
- [聚合统计查询](#聚合统计查询)
- [性能优化查询](#性能优化查询)
- [动态数据源](#动态数据源)
- [实际项目示例](#实际项目示例)
- [最佳实践](#最佳实�?
- [常见问题](#常见问题)

---

## 快速开�?

### 为什么选择 Synapse Framework�?

想象一下，你不需要写这样的代码：
```java
// 传统方式 - 繁琐且容易出�?
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("username", username);
wrapper.eq("status", 1);
wrapper.ge("create_time", startTime);
wrapper.le("create_time", endTime);
wrapper.orderByDesc("create_time");
List<User> users = userService.list(wrapper);
```

而是这样�?
```java
// Synapse 方式 - 简洁优�?
UserQueryDTO query = new UserQueryDTO();
query.setUsername(username);
query.setStatus(1);
query.setStartTime(startTime);
query.setEndTime(endTime);
query.setOrderByList(Arrays.asList(new PageDTO.OrderBy("create_time", "DESC")));

List<User> users = userRepository.listWithDTO(query);
```

**这就�?Synapse 的魅力！** 🎯

### 核心特�?

- 🚀 **开箱即�?* - 继承 `BaseRepository` 即可获得所�?MyBatis-Plus 功能
- 🎯 **智能查询** - 基于注解的自动查询条件构�?
- 📊 **多种分页** - 支持基础分页、聚合分页、性能监控分页
- 🔗 **多表关联** - 支持 INNER、LEFT、RIGHT、FULL JOIN
- �?**性能优化** - 内置查询性能监控和优化建�?
- 🛡�?**企业�?* - 动态数据源、审计功能、事务管�?

---

## 基础配置

### 1. Maven 依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-databases</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

```yaml
# application.yml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        cache-enabled: true
        lazy-loading-enabled: true
      global-config:
        banner: false
        enable-sql-runner: false
        enable-meta-object-handler: true
        enable-sql-injector: true
        enable-pagination: true
        enable-optimistic-locker: true
        enable-block-attack: true
      type-aliases-package: com.indigo.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
    primary: master1
    dynamic-data-source:
      strict: false
      seata: false
      p6spy: false
      datasource:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
          params:
            useUnicode: "true"
            characterEncoding: "utf8"
            useSSL: "false"
            serverTimezone: "Asia/Shanghai"
          hikari:
            minimum-idle: 5
            maximum-pool-size: 15
            idle-timeout: 30000
            max-lifetime: 1800000
            connection-timeout: 30000
            connection-test-query: "SELECT 1"
```

### 3. 启动类配�?

```java
@SpringBootApplication
@EnableSynapseDatabases  // 启用 Synapse 数据库模�?
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 实体类设�?

### 1. 基础实体�?

```java
// 基础实体�?- 包含主键
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BaseEntity<T> implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private T id;
}

// 创建审计实体�?- 包含创建信息
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CreatedEntity<T> extends BaseEntity<T> {
    @TableField(fill = FieldFill.INSERT, value = "create_time")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE, value = "create_user")
    private T createUser;
}

// 完整审计实体�?- 包含创建和修改信�?
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AuditEntity<T> extends CreatedEntity<T> {
    @Version
    private Integer revision;
    
    @TableLogic(delval = "0", value = "1")
    private Boolean deleted;
    
    @TableField(fill = FieldFill.INSERT, value = "modify_time")
    private LocalDateTime modifyTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE, value = "modify_user")
    private T modifyUser;
}
```

### 2. 业务实体类示�?

```java
// 用户实体�?
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends AuditEntity<String> {
    
    @QueryCondition(type = QueryCondition.QueryType.LIKE)
    private String username;
    
    private String password;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Integer status;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Boolean locked;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Boolean enabled;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Boolean expired;
    
    @QueryCondition(type = QueryCondition.QueryType.GE, field = "last_login_time")
    private LocalDateTime lastLoginTime;
}
```

### 3. 查询 DTO 设计

```java
// 用户查询 DTO
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageDTO {
    
    @QueryCondition(type = QueryCondition.QueryType.LIKE)
    private String username;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Integer status;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Boolean locked;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Boolean enabled;
    
    @QueryCondition(type = QueryCondition.QueryType.GE, field = "create_time")
    private LocalDateTime startTime;
    
    @QueryCondition(type = QueryCondition.QueryType.LE, field = "create_time")
    private LocalDateTime endTime;
    
    @QueryCondition(type = QueryCondition.QueryType.IN)
    private List<Long> deptIds;
}
```

---

## Repository层开�?

### 1. 基础 Repository 接口

```java
// 用户 Repository 接口
@AutoRepository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    // 什么都不用写！所有基础功能都有�?
    // 继承 BaseRepository 即可获得�?
    // - 所�?MyBatis-Plus IService 方法
    // - 自动查询条件构建
    // - 多种分页查询方式
    // - 聚合统计查询
    // - 性能监控查询
}
```

### 2. 基础 CRUD 操作

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 插入 - 自动填充审计字段
    public void createUser(User user) {
        userRepository.save(user); // 自动设置 createTime, updateTime, createBy �?
    }
    
    // 批量插入 - 性能优化
    public void batchCreateUsers(List<User> users) {
        userRepository.saveBatch(users, 1000); // 分批插入，避免内存溢�?
    }
    
    // 更新 - 自动填充 updateTime
    public void updateUser(User user) {
        userRepository.updateById(user); // 自动设置 updateTime
    }
    
    // 删除 - 逻辑删除
    public void deleteUser(String id) {
        userRepository.removeById(id); // 自动设置 deleted = 1
    }
    
    // 根据ID查询
    public User getUserById(String id) {
        return userRepository.getById(id);
    }
    
    // 批量查询
    public List<User> getUsersByIds(List<String> ids) {
        return userRepository.listByIds(ids);
    }
}
```

### 3. 自定义查询方�?

```java
@AutoRepository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 自定义查询方�?- 使用 @Select 注解
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    // 动态查�?- 使用 @Select �?<script> 标签
    @Select("""
        <script>
        SELECT * FROM sys_user 
        <where>
            deleted = 0
            <if test="username != null and username != ''">
                AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
        ORDER BY create_time DESC
        </script>
        """)
    List<User> findUsersWithDynamicCondition(@Param("username") String username, 
                                            @Param("status") Integer status);
}
```

---

## 查询条件构建

### 1. @QueryCondition 注解详解

```java
// 支持的查询类�?
public enum QueryType {
    EQ,           // 等于 (=)
    NE,           // 不等�?(!=)
    LIKE,         // 模糊查询 (LIKE '%value%')
    LIKE_LEFT,    // 左模�?(LIKE '%value')
    LIKE_RIGHT,   // 右模�?(LIKE 'value%')
    GT,           // 大于 (>)
    GE,           // 大于等于 (>=)
    LT,           // 小于 (<)
    LE,           // 小于等于 (<=)
    IN,           // IN查询 (IN (...))
    NOT_IN,       // NOT IN查询 (NOT IN (...))
    BETWEEN,      // 范围查询 (BETWEEN ... AND ...)
    IS_NULL,      // IS NULL
    IS_NOT_NULL   // IS NOT NULL
}
```

### 2. 自动查询条件构建

```java
// 传统方式 vs Synapse 方式
public class UserService {
    
    // �?传统方式 - 手写 QueryWrapper
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
    
    // �?Synapse 方式 - 自动构建查询条件
    public List<User> findUsersNew(String username, Integer status, Date startTime) {
        UserQueryDTO query = new UserQueryDTO();
        query.setUsername(username);
        query.setStatus(status);
        query.setStartTime(startTime);
        
        return userRepository.listWithDTO(query); // 自动构建 QueryWrapper�?
    }
}
```

---

## 分页查询

### 1. 基础分页查询

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
        
        // 支持多字段排�?
        query.setOrderByList(Arrays.asList(
            new PageDTO.OrderBy("status", "ASC"),      // 先按状态升�?
            new PageDTO.OrderBy("create_time", "DESC") // 再按创建时间降序
        ));
        
        return userRepository.pageWithCondition(query);
    }
}
```

### 2. PageResult 结果对象

```java
// 分页结果对象
@Data
public class PageResult<T> {
    private List<T> records;     // 数据列表
    private Long total;          // 总记录数
    private Long current;        // 当前页码
    private Long size;           // 每页大小
    private Long pages;          // 总页�?
    private Boolean hasNext;      // 是否有下一�?
    private Boolean hasPrevious; // 是否有上一�?
}
```

---

## 多表关联查询

### 1. 使用框架提供的多表关�?

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 使用框架�?JoinPageDTO - 简单场�?
    default PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        // 配置多表关联
        queryDTO.setTableJoins(Arrays.asList(
            new JoinPageDTO.TableJoin("sys_department", "d", JoinPageDTO.JoinType.LEFT, "u.dept_id = d.id"),
            new JoinPageDTO.TableJoin("sys_role", "r", JoinPageDTO.JoinType.LEFT, "u.role_id = r.id"),
            new JoinPageDTO.TableJoin("sys_user_profile", "p", JoinPageDTO.JoinType.LEFT, "u.id = p.user_id")
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

---

## 聚合统计查询

### 1. 基础聚合查询

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 用户行为分析 - 多维度统�?
    default AggregationPageResult<User> getUserBehaviorAnalysis() {
        AggregationPageDTO queryDTO = new AggregationPageDTO();
        
        // 设置聚合字段
        queryDTO.setAggregations(Arrays.asList(
            new AggregationPageDTO.AggregationField("id", AggregationPageDTO.AggregationType.COUNT, "total_users"),
            new AggregationPageDTO.AggregationField("status", AggregationPageDTO.AggregationType.COUNT, "active_users"),
            new AggregationPageDTO.AggregationField("create_time", AggregationPageDTO.AggregationType.MAX, "latest_user_time"),
            new AggregationPageDTO.AggregationField("create_time", AggregationPageDTO.AggregationType.MIN, "earliest_user_time")
        ));
        
        // 设置分组字段
        queryDTO.setGroupByFields(Arrays.asList("dept_id", "role_id"));
        
        return pageWithAggregation(queryDTO);
    }
}
```

### 2. 聚合查询 DTO

```java
// 聚合查询 DTO
@Data
@EqualsAndHashCode(callSuper = true)
public class AggregationPageDTO extends PageDTO {
    
    // 聚合统计字段
    private List<AggregationField> aggregations;
    
    // 分组字段
    private List<String> groupByFields;
    
    @Data
    public static class AggregationField {
        private String field;           // 字段�?
        private AggregationType type;   // 聚合类型
        private String alias;           // 别名
        
        public AggregationField(String field, AggregationType type) {
            this.field = field;
            this.type = type;
            this.alias = type.name().toLowerCase() + "_" + field;
        }
        
        public AggregationField(String field, AggregationType type, String alias) {
            this.field = field;
            this.type = type;
            this.alias = alias;
        }
    }
    
    // 聚合类型枚举
    public enum AggregationType {
        COUNT,           // 计数
        SUM,             // 求和
        AVG,             // 平均�?
        MAX,             // 最大�?
        MIN,             // 最小�?
        COUNT_DISTINCT   // 去重计数
    }
}
```

### 3. 聚合结果对象

```java
// 聚合分页结果
@Data
@EqualsAndHashCode(callSuper = true)
public class AggregationPageResult<T> extends PageResult<T> {
    
    // 聚合统计结果
    private Map<String, Object> aggregations;
    
    // 分组统计结果
    private List<Map<String, Object>> groupResults;
    
    public static <T> AggregationPageResult<T> withAggregations(
            List<T> records, Long total, Long current, Long size, 
            Map<String, Object> aggregations) {
        AggregationPageResult<T> result = new AggregationPageResult<>(records, total, current, size);
        result.setAggregations(aggregations);
        return result;
    }
}
```

---

## 性能优化查询

### 1. 性能监控查询

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 性能监控查询 - 返回查询执行时间和执行计�?
    default PerformancePageResult<User> getUserPerformanceAnalysis(UserQueryDTO queryDTO) {
        PerformancePageDTO performanceDTO = new PerformancePageDTO();
        
        // 设置选择字段 - 只查询需要的字段
        performanceDTO.setSelectFields(Arrays.asList("id", "username", "email", "status", "create_time"));
        
        // 设置查询条件
        performanceDTO.setUsername(queryDTO.getUsername());
        performanceDTO.setStatus(queryDTO.getStatus());
        
        return pageWithPerformance(performanceDTO);
    }
}
```

### 2. 字段选择优化

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 字段选择查询 - 只查询需要的字段，提高查询性能
    default PerformancePageResult<User> getUsersOptimized(Integer status, Integer limit) {
        PerformancePageDTO queryDTO = new PerformancePageDTO();
        
        // 只选择必要字段
        queryDTO.setSelectFields(Arrays.asList("id", "username", "email", "status"));
        queryDTO.setStatus(status);
        queryDTO.setPageSize(limit);
        
        return pageWithSelectFields(queryDTO);
    }
}
```

### 3. 缓存查询

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 缓存查询 - 支持查询结果缓存
    default PerformancePageResult<User> getUsersWithCache(UserQueryDTO queryDTO) {
        PerformancePageDTO performanceDTO = new PerformancePageDTO();
        
        // 设置缓存相关参数
        performanceDTO.setEnableCache(true);
        performanceDTO.setCacheKey("users:" + queryDTO.hashCode());
        performanceDTO.setCacheExpire(300); // 5分钟过期
        
        return pageWithCache(performanceDTO);
    }
}
```

---

## 动态数据源

### 1. 数据源配�?

```yaml
synapse:
  datasource:
    primary: master1
    dynamic-data-source:
      strict: false
      seata: false
      p6spy: false
      datasource:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
          role: MASTER
        slave1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam_slave
          username: root
          password: your_password
          pool-type: HIKARI
          role: SLAVE
```

### 2. 数据源切�?

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 从主库查�?
    @DS("master1")
    default List<User> getUsersFromMaster() {
        return list();
    }
    
    // 从从库查�?
    @DS("slave1")
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

### 3. 读写分离

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 写操�?- 自动路由到主�?
    @Transactional
    public void createUser(User user) {
        userRepository.save(user); // 自动路由到主�?
    }
    
    // 读操�?- 自动路由到从�?
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.list(); // 自动路由到从�?
    }
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
    @QueryCondition(type = QueryCondition.QueryType.LIKE)
    private String username;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private String email;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private String phone;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private Integer status;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private String deptName;
    
    @QueryCondition(type = QueryCondition.QueryType.EQ)
    private String roleName;
    
    @QueryCondition(type = QueryCondition.QueryType.GE, field = "create_time")
    private Date startTime;
    
    @QueryCondition(type = QueryCondition.QueryType.LE, field = "create_time")
    private Date endTime;
    
    @QueryCondition(type = QueryCondition.QueryType.IN)
    private List<Long> deptIds;
    
    @QueryCondition(type = QueryCondition.QueryType.IN)
    private List<Long> roleIds;
    
    // 业务字段
    private Integer minOrderCount;      // 最小订单数
    private Integer maxOrderCount;      // 最大订单数
    private BigDecimal minTotalAmount;  // 最小消费金�?
    private BigDecimal maxTotalAmount;  // 最大消费金�?
    private String lastLoginIp;         // 最后登录IP
    private String userLevel;           // 用户等级
}

// 用户 Repository
@AutoRepository
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

## 最佳实�?

### 1. 命名规范

```java
// �?好的命名
@AutoRepository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    // 查询方法�?select 开�?
    List<User> selectActiveUsers();
    
    // 统计方法�?count 开�?
    long countUsersByStatus(Integer status);
    
    // 检查方法以 has �?exists 开�?
    boolean hasUserWithEmail(String email);
    
    // 更新方法�?update 开�?
    int updateUserStatus(String userId, Integer status);
}

// �?不好的命�?
public interface UserRepository extends BaseRepository<User, UserMapper> {
    List<User> getActiveUsers();        // 应该�?select
    long getCountByStatus(Integer status); // 应该�?count
    boolean checkEmail(String email);   // 应该�?has �?exists
}
```

### 2. 查询优化原则

```java
@AutoRepository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // �?好的做法
    @Select("""
        SELECT u.id, u.username, u.email  -- 只选择需要的字段
        FROM sys_user u
        WHERE u.deleted = 0               -- 使用索引字段
            AND u.status = #{status}       -- 使用索引字段
        ORDER BY u.create_time DESC        -- 使用索引排序
        LIMIT #{pageSize}                  -- 限制结果集大�?
        """)
    List<User> selectUsersOptimized(@Param("status") Integer status, 
                                   @Param("pageSize") Integer pageSize);
    
    // �?不好的做�?
    @Select("""
        SELECT *                          -- 选择所有字�?
        FROM sys_user u
        WHERE u.username LIKE '%admin%'   -- 不使用索引的模糊查询
        ORDER BY u.email                  -- 不使用索引排�?
        """)
    List<User> selectUsersBad();
}
```

### 3. 异常处理

```java
@AutoRepository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // �?好的异常处理
    default User getUserByIdSafely(String userId) {
        try {
            return getById(userId);
        } catch (Exception e) {
            log.error("查询用户失败，用户ID: {}", userId, e);
            throw new BusinessException("查询用户失败: " + e.getMessage());
        }
    }
    
    // �?使用 Optional 处理空�?
    default Optional<User> getUserByIdOptional(String userId) {
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

## 常见问题

### 1. N+1 查询问题

```java
// �?问题代码 - 会产�?N+1 查询
public List<UserDetailDTO> getUsersWithDetails(List<String> userIds) {
    List<UserDetailDTO> result = new ArrayList<>();
    for (String userId : userIds) {
        User user = getById(userId);                    // 1次查�?
        Department dept = getDeptById(user.getDeptId()); // N次查�?
        Role role = getRoleById(user.getRoleId());       // N次查�?
        result.add(new UserDetailDTO(user, dept, role));
    }
    return result;
}

// �?解决方案 - 使用 JOIN 查询
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
List<UserDetailDTO> selectUsersWithDetails(@Param("userIds") List<String> userIds);
```

### 2. 深度分页问题

```java
// �?问题代码 - 深度分页性能�?
@Select("""
    SELECT * FROM sys_user 
    ORDER BY create_time DESC 
    LIMIT #{offset}, #{pageSize}
    """)
List<User> selectUsersWithOffset(@Param("offset") Integer offset, 
                                 @Param("pageSize") Integer pageSize);

// �?解决方案 - 使用游标分页
@Select("""
    SELECT * FROM sys_user 
    WHERE id > #{lastId}  -- 使用 ID 作为游标
    ORDER BY id ASC        -- �?ID 排序
    LIMIT #{pageSize}
    """)
List<User> selectUsersWithCursor(@Param("lastId") String lastId, 
                                 @Param("pageSize") Integer pageSize);
```

### 3. 大数据量处理

```java
// �?问题代码 - 一次性加载所有数�?
@Select("SELECT * FROM sys_user")
List<User> selectAllUsers();

// �?解决方案 - 分批处理
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

Synapse Framework 数据库模块为你提供了�?

🎯 **开箱即用的基础功能** - 继承 `BaseRepository` 就能获得所�?MyBatis-Plus 功能
🚀 **智能查询构建** - 告别手写 QueryWrapper，让代码更优�?
🔗 **灵活的多表关�?* - 支持框架方法和手�?SQL 两种方式
�?**性能优化工具** - 内置分页、缓存、索引优化等最佳实�?
🛡�?**企业级特�?* - 审计、事务、动态数据源等生产环境必需功能

记住�?*好的代码不是写出来的，而是设计出来�?*。Synapse Framework 帮你专注于业务逻辑，而不是重复的 CRUD 代码�?

现在，去写那些真正有价值的业务代码吧！🚀
