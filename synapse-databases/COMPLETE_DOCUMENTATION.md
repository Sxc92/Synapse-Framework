# Synapse Framework - 数据库模块完整文档

## 📋 **概述**

Synapse Framework 数据库模块是一个集成了 MyBatis-Plus 和动态数据源的强大数据库解决方案。它提供了灵活的配置选项，支持多种数据库类型和连接池，并且兼容标准的 Spring Boot 配置格式。

## 🚀 **主要特性**

- 🚀 **MyBatis-Plus 集成**: 完整的 MyBatis-Plus 配置支持，使用 MybatisSqlSessionFactoryBean
- 🔄 **动态数据源**: 支持多数据源动态切换，带配置验证和健康检查
- 🗄️ **多数据库支持**: MySQL, PostgreSQL, Oracle, SQL Server, H2
- 🏊 **连接池支持**: HikariCP, Druid
- ⚙️ **灵活配置**: 支持自定义配置和默认值
- 🔌 **Spring Boot 兼容**: 兼容标准 Spring Boot 配置格式
- 🎯 **BaseRepository**: 强大的Repository接口，支持VO映射、多表关联查询
- 🔍 **EnhancedQueryBuilder**: 增强查询构建器，支持聚合查询、性能监控
- 🤖 **@AutoRepository**: 自动Repository注解，无需手动实现
- 🔒 **自动字段填充**: 支持审计字段自动填充（创建时间、修改时间、用户信息、乐观锁、逻辑删除）
- ✅ **配置验证**: 启动时自动验证数据源配置和连接性
- 🔧 **问题修复**: 修复了MyBatis绑定异常、数据源验证、字段填充等关键问题

---

## 📖 **目录**

1. [基础配置](#基础配置)
2. [BaseRepository 使用指南](#baserepository-使用指南)
3. [EnhancedQueryBuilder 使用指南](#enhancedquerybuilder-使用指南)
4. [@AutoRepository 使用指南](#autorepository-使用指南)
5. [多表查询方式对比](#多表查询方式对比)
6. [配置属性说明](#配置属性说明)
7. [自动字段填充](#自动字段填充)
8. [配置验证](#配置验证)
9. [问题修复记录](#问题修复记录)
10. [性能优化建议](#性能优化建议)
11. [最佳实践](#最佳实践)
12. [常见问题](#常见问题)

---

## 🔧 **基础配置**

### **配置前缀**

新的配置类使用 `synapse.datasource` 作为配置前缀，替代了之前的 `synapse.databases`。

### **主要配置结构**

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        # ... 其他 MyBatis-Plus 配置
      global-config:
        banner: false
        enable-pagination: true
        # ... 其他全局配置
      type-aliases-package: com.indigo.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    dynamic-data-source:
      primary: master1
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

### **兼容性配置**

为了保持向后兼容性，模块仍然支持标准的 Spring Boot 配置格式：

```yaml
spring:
  datasource:
    dynamic:
      primary: master1
      strict: false
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

### **数据库类型支持**

| 数据库类型 | 枚举值 | 驱动类 | 默认端口 |
|------------|--------|---------|----------|
| MySQL | MYSQL | com.mysql.cj.jdbc.Driver | 3306 |
| PostgreSQL | POSTGRESQL | org.postgresql.Driver | 5432 |
| Oracle | ORACLE | oracle.jdbc.OracleDriver | 1521 |
| SQL Server | SQLSERVER | com.microsoft.sqlserver.jdbc.SQLServerDriver | 1433 |
| H2 | H2 | org.h2.Driver | 8082 |

### **连接池类型支持**

| 连接池类型 | 枚举值 | 说明 |
|------------|--------|------|
| HikariCP | HIKARI | 高性能连接池，Spring Boot 默认 |
| Druid | DRUID | 阿里巴巴开源连接池，功能丰富 |

### **使用示例**

#### **1. 基本配置**

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
      global-config:
        banner: false
        enable-pagination: true
    primary: master
    dynamic-data-source:
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: test_db
          username: root
          password: password
          pool-type: HIKARI
```

#### **2. 多数据源配置**

```yaml
synapse:
  datasource:
    dynamic-data-source:
      primary: master
      datasource:
        master:
          type: MYSQL
          host: master-host
          database: master_db
          username: user
          password: pass
          pool-type: HIKARI
        slave:
          type: MYSQL
          host: slave-host
          database: slave_db
          username: user
          password: pass
          pool-type: HIKARI
```

#### **3. 高级连接池配置**

```yaml
synapse:
  datasource:
    dynamic-data-source:
      datasource:
        master:
          type: MYSQL
          host: localhost
          database: test_db
          username: root
          password: password
          pool-type: HIKARI
          hikari:
            minimum-idle: 10
            maximum-pool-size: 50
            idle-timeout: 600000
            max-lifetime: 3600000
            connection-timeout: 60000
            connection-test-query: "SELECT 1"
            leak-detection-threshold: 300000
```

### **代码中使用**

#### **动态切换数据源**

```java
@Service
public class UserService {
    
    @DS("slave") // 使用 @DS 注解切换数据源
    public List<User> getUsersFromSlave() {
        return userMapper.selectList(null);
    }
    
    @DS("master") // 切换到主数据源
    public void saveUser(User user) {
        userMapper.insert(user);
    }
}
```

#### **编程式切换数据源**

```java
@Service
public class UserService {
    
    public List<User> getUsersFromSlave() {
        DynamicDataSourceContextHolder.setDataSource("slave");
        try {
            return userMapper.selectList(null);
        } finally {
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
}
```

---

## 🎯 **BaseRepository 使用指南**

### **概述**

`BaseRepository` 是一个强大的Repository接口，继承MyBatis-Plus的IService，提供完整的CRUD功能和增强查询能力。支持VO映射、多表关联查询、聚合查询、性能监控等功能。

> ⚠️ **重要说明**：`BaseRepository`是一个接口，需要使用`@AutoRepository`注解标记，框架会自动生成代理实现。不要使用`extends BaseRepositoryImpl`的方式，因为`BaseRepositoryImpl`类不存在。

> 📝 **配置说明**：确保在`application.yml`中配置正确的包扫描路径，否则Spring Bean可能找不到。

### **主要功能**

#### **1. 基础CRUD功能**
- 继承MyBatis-Plus的IService所有功能
- 支持自动查询条件构建
- 支持@QueryCondition注解

#### **2. VO映射支持**
- 所有查询方法都支持直接映射到VO对象
- 智能字段选择，避免内存转换
- 支持单表和多表查询

#### **3. 增强查询功能**
- 多表关联查询
- 聚合查询（COUNT、SUM、AVG等）
- 性能监控查询
- 复杂查询支持

#### **4. 便捷方法**
- 快速查询方法
- 统计查询
- 存在性查询

### **使用示例**

#### **基础分页查询**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 基础分页查询 - 返回实体
     */
    PageResult<Product> pageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 基础分页查询 - 返回VO
     */
    PageResult<ProductVO> pageProductsAsVO(ProductPageQueryDTO queryDTO);
}
```

#### **列表查询**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 列表查询 - 返回实体
     */
    List<Product> listProducts(ProductQueryDTO queryDTO);
    
    /**
     * 列表查询 - 返回VO
     */
    List<ProductVO> listProductsAsVO(ProductQueryDTO queryDTO);
}
```

#### **多表关联查询**

##### **方式1：基于@VoMapping注解（推荐）**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 多表关联查询 - 基于@VoMapping注解（推荐）
     * 自动根据ProductMultiTableVO的@VoMapping注解配置进行多表关联查询
     */
    PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
    
    /**
     * 基础分页查询 - 也支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> pageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 便捷查询 - 支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
}
```

##### **方式2：基于JoinPageDTO配置（已过时）**

> ⚠️ **注意：此方式已标记为过时，推荐使用@VoMapping注解方式**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 多表关联查询 - 基于JoinPageDTO配置（已过时）
     * @deprecated 推荐使用 {@link #pageWithVoMapping(PageDTO, Class)} 或 {@link #pageWithCondition(PageDTO, Class)}
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    PageResult<ProductMultiTableVO> pageProductsWithJoin(JoinPageDTO joinPageDTO);
}
```

#### **聚合查询**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 聚合查询
     */
    AggregationPageResult<ProductVO> getProductStatistics(AggregationPageDTO aggregationPageDTO);
}
```

#### **性能监控查询**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 性能监控查询
     */
    PerformancePageResult<ProductVO> pageProductsWithPerformance(PerformancePageDTO performancePageDTO);
}
```

#### **便捷查询方法**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 快速分页查询 - 支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 快速列表查询 - 支持@VoMapping注解的多表关联
     */
    List<ProductMultiTableVO> quickListProducts(ProductQueryDTO queryDTO);
    
    /**
     * 快速单个查询 - 支持@VoMapping注解的多表关联
     */
    ProductMultiTableVO quickGetProduct(ProductQueryDTO queryDTO);
    
    /**
     * 统计查询 - 支持@VoMapping注解的多表关联
     */
    Long countProducts(ProductQueryDTO queryDTO);
    
    /**
     * 存在性查询 - 支持@VoMapping注解的多表关联
     */
    boolean existsProduct(ProductQueryDTO queryDTO);
}
```

### **方法对比**

| 方法类型 | 实体返回 | VO返回 | 性能 | 推荐度 |
|---------|---------|--------|------|--------|
| `pageWithCondition()` | ✅ | ✅ | 高 | ⭐⭐⭐⭐⭐ |
| `listWithDTO()` | ✅ | ✅ | 高 | ⭐⭐⭐⭐⭐ |
| `getOneWithDTO()` | ✅ | ✅ | 高 | ⭐⭐⭐⭐⭐ |
| `quickPage()` | ❌ | ✅ | 最高 | ⭐⭐⭐⭐⭐ |
| `quickList()` | ❌ | ✅ | 最高 | ⭐⭐⭐⭐⭐ |
| `quickGetOne()` | ❌ | ✅ | 最高 | ⭐⭐⭐⭐⭐ |

---

## 🔍 **EnhancedQueryBuilder 使用指南**

### **概述**

`EnhancedQueryBuilder` 是一个强大的查询构建器，基于 MyBatis-Plus 提供便捷的查询方法。支持单表查询、多表关联查询、聚合查询、性能监控等功能。

### **主要功能**

#### **1. 基础查询**
- 分页查询：`pageWithCondition()`
- 列表查询：`listWithCondition()`
- 单个查询：`getOneWithCondition()`

#### **2. 多表关联查询**
- 支持 INNER、LEFT、RIGHT、FULL JOIN
- 自动处理单表/多表查询路由
- 支持VO字段映射

#### **3. 聚合查询**
- COUNT、SUM、AVG、MAX、MIN等聚合函数
- 分组查询支持
- 聚合结果统计

#### **4. 性能监控**
- 查询执行时间统计
- 执行计划分析
- 性能评级（优秀/良好/一般/需要优化）

#### **5. 便捷方法**
- 快速查询：`quickPage()`, `quickList()`, `quickGetOne()`
- 统计查询：`countWithCondition()`
- 存在性查询：`existsWithCondition()`

### **使用示例**

#### **基础分页查询**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 基础分页查询
     */
    public PageResult<ProductVO> pageProducts(ProductPageQueryDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithCondition(productRepository, queryDTO, ProductVO.class);
    }
    
    /**
     * 便捷分页查询
     */
    public PageResult<ProductVO> quickPageProducts(ProductPageQueryDTO queryDTO) {
        return EnhancedQueryBuilder.quickPage(productRepository, queryDTO, ProductVO.class);
    }
}
```

#### **多表关联查询**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 多表关联查询
     */
    public PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithCondition(productRepository, queryDTO, ProductMultiTableVO.class);
    }
}
```

#### **聚合查询**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 聚合查询
     */
    public AggregationPageResult<ProductVO> getProductStatistics(ProductPageQueryDTO queryDTO) {
        AggregationPageDTO aggregationPageDTO = new AggregationPageDTO();
        aggregationPageDTO.setPageNo(queryDTO.getPageNo());
        aggregationPageDTO.setPageSize(queryDTO.getPageSize());
        
        // 添加聚合字段
        List<AggregationPageDTO.AggregationField> aggregations = Arrays.asList(
            new AggregationPageDTO.AggregationField("price", AggregationPageDTO.AggregationType.AVG, "avg_price"),
            new AggregationPageDTO.AggregationField("stock", AggregationPageDTO.AggregationType.SUM, "total_stock"),
            new AggregationPageDTO.AggregationField("id", AggregationPageDTO.AggregationType.COUNT, "total_count")
        );
        aggregationPageDTO.setAggregations(aggregations);
        
        return EnhancedQueryBuilder.pageWithAggregation(productRepository, aggregationPageDTO, ProductVO.class);
    }
}
```

#### **性能监控查询**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 性能监控查询
     */
    public PerformancePageResult<ProductVO> pageProductsWithPerformance(ProductPageQueryDTO queryDTO) {
        PerformancePageDTO performancePageDTO = new PerformancePageDTO();
        performancePageDTO.setPageNo(queryDTO.getPageNo());
        performancePageDTO.setPageSize(queryDTO.getPageSize());
        performancePageDTO.setExplain(true); // 显示执行计划
        performancePageDTO.setUseCache(true); // 使用缓存
        
        return EnhancedQueryBuilder.pageWithPerformance(productRepository, performancePageDTO, ProductVO.class);
    }
}
```

#### **统计和存在性查询**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 统计查询
     */
    public Long countProducts(ProductQueryDTO queryDTO) {
        return EnhancedQueryBuilder.countWithCondition(productRepository, queryDTO, ProductVO.class);
    }
    
    /**
     * 存在性查询
     */
    public boolean existsProduct(ProductQueryDTO queryDTO) {
        return EnhancedQueryBuilder.existsWithCondition(productRepository, queryDTO, ProductVO.class);
    }
}
```

### **性能评级标准**

- **优秀**：查询时间 < 100ms
- **良好**：查询时间 < 500ms
- **一般**：查询时间 < 1000ms
- **需要优化**：查询时间 >= 1000ms

---

## 🤖 **@AutoRepository 使用指南**

### **问题说明**

仅仅使用`@AutoRepository`注解是不够的，还需要确保Spring能够正确扫描和注册这些Bean。

### **解决方案**

我已经创建了完善的自动配置机制来解决这个问题：

#### **1. 自动配置类**

创建了`AutoRepositoryConfiguration`类，提供：
- 自动扫描带有`@AutoRepository`注解的接口
- 自动生成代理实现
- 自动注册为Spring Bean
- 支持可配置的包扫描路径

#### **2. 配置属性**

创建了`AutoRepositoryProperties`类，支持：
- 启用/禁用自动Repository功能
- 配置扫描包路径
- 配置Bean名称生成策略
- 启用调试日志

### **使用方法**

#### **1. 基础使用**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 多表关联查询 - 基于@VoMapping注解（推荐）
     */
    PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
    
    /**
     * 便捷查询 - 支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
}
```

#### **2. 配置说明**

在`application.yml`中配置：

```yaml
synapse:
  databases:
    auto-repository:
      enabled: true  # 启用自动Repository功能（默认true）
      base-packages:  # 扫描的包路径
        - com.indigo
        - com.yourcompany
        - com.example
      debug: false   # 启用调试日志（默认false）
      bean-name-strategy: SIMPLE_NAME  # Bean名称生成策略
```

#### **3. 配置选项说明**

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用自动Repository功能 |
| `base-packages` | `["com.indigo", "com.yourcompany", "com.example"]` | 扫描的包路径列表 |
| `debug` | `false` | 是否启用调试日志 |
| `bean-name-strategy` | `SIMPLE_NAME` | Bean名称生成策略 |

#### **4. Bean名称生成策略**

- **`SIMPLE_NAME`**: 使用简单类名（如：`ProductRepository`）
- **`FULL_NAME`**: 使用完整类名（如：`com.indigo.repository.ProductRepository`）
- **`CAMEL_CASE`**: 使用驼峰命名（如：`productRepository`）

### **工作原理**

1. **扫描阶段**: 启动时扫描配置的包路径，查找带有`@AutoRepository`注解的接口
2. **代理生成**: 为每个找到的接口创建动态代理
3. **Bean注册**: 将代理对象注册为Spring Bean
4. **自动注入**: Spring容器启动后，可以通过`@Autowired`注入使用

### **注意事项**

#### **1. 包扫描路径**

确保你的Repository接口所在的包在配置的`base-packages`中：

```yaml
synapse:
  databases:
    auto-repository:
      base-packages:
        - com.indigo.repository  # 你的Repository包路径
        - com.yourcompany.repository
```

#### **2. 接口命名**

建议Repository接口使用`Repository`后缀，这样Bean名称更清晰：

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    // 方法定义
}
```

#### **3. 依赖注入**

在Service中正常使用`@Autowired`注入：

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;  // 自动注入代理实现
    
    public PageResult<ProductMultiTableVO> getProducts(ProductPageQueryDTO queryDTO) {
        return productRepository.pageProductsWithBrand(queryDTO);
    }
}
```

### **完整示例**

#### **1. Repository接口**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 基础分页查询
     */
    PageResult<Product> pageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 多表关联查询 - 基于@VoMapping注解
     */
    PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
    
    /**
     * 便捷查询
     */
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
}
```

#### **2. Service实现**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public PageResult<Product> getProducts(ProductPageQueryDTO queryDTO) {
        return productRepository.pageProducts(queryDTO);
    }
    
    public PageResult<ProductMultiTableVO> getProductsWithBrand(ProductPageQueryDTO queryDTO) {
        return productRepository.pageProductsWithBrand(queryDTO);
    }
}
```

#### **3. 配置文件**

```yaml
synapse:
  databases:
    auto-repository:
      enabled: true
      base-packages:
        - com.indigo.repository
        - com.yourcompany.repository
      debug: true  # 开发环境可以启用调试日志
```

---

## 📊 **多表查询方式对比**

### **问题说明**

`ProductMultiTableVO`示例展示的是使用`@VoMapping`注解配置多表关联，但是`BaseRepository`中的方法调用方式与这个注解配置不匹配。

### **解决方案**

我已经为`BaseRepository`添加了专门支持`@VoMapping`注解的方法，现在有两种多表查询方式：

> ⚠️ **重要说明**：`BaseRepository`是一个接口，需要使用`@AutoRepository`注解标记，框架会自动生成代理实现。不要使用`extends BaseRepositoryImpl`的方式，因为`BaseRepositoryImpl`类不存在。

#### **方式1：基于@VoMapping注解（推荐）**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 多表关联查询 - 基于@VoMapping注解（推荐）
     * 自动根据ProductMultiTableVO的@VoMapping注解配置进行多表关联查询
     */
    PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
    
    /**
     * 基础分页查询 - 也支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> pageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 便捷查询 - 支持@VoMapping注解的多表关联
     */
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
}
```

#### **方式2：基于JoinPageDTO配置（已过时）**

> ⚠️ **注意：此方式已标记为过时，推荐使用@VoMapping注解方式**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 多表关联查询 - 基于JoinPageDTO配置（已过时）
     * @deprecated 推荐使用 {@link #pageWithVoMapping(PageDTO, Class)} 或 {@link #pageWithCondition(PageDTO, Class)}
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    PageResult<ProductMultiTableVO> pageProductsWithJoin(JoinPageDTO joinPageDTO);
}
```

### **方式对比**

| 特性 | @VoMapping注解方式 | JoinPageDTO配置方式（已过时） |
|------|-------------------|---------------------------|
| **配置方式** | 在VO类上使用注解 | 在代码中配置DTO |
| **代码简洁性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **维护性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **灵活性** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **推荐度** | ⭐⭐⭐⭐⭐ | ❌ 已过时 |
| **状态** | ✅ 推荐使用 | ⚠️ 已标记过时 |

### **推荐使用方式**

#### **✅ 推荐：@VoMapping注解方式**

**优势：**
1. **配置简单**：在VO类上使用注解即可
2. **代码简洁**：Service层代码非常简洁
3. **维护性好**：多表关联配置集中在VO类中
4. **自动路由**：框架自动判断单表/多表查询
5. **类型安全**：编译时类型检查

**使用场景：**
- 固定的多表关联关系
- 常用的查询场景
- 需要代码简洁的项目

#### **❌ 不推荐：JoinPageDTO配置方式（已过时）**

> ⚠️ **此方式已标记为过时，将在未来版本中移除**

**原因：**
1. **代码冗余**：需要大量配置代码
2. **维护困难**：关联配置分散在业务代码中
3. **容易出错**：手动配置容易出错
4. **不够优雅**：相比注解方式不够优雅

**迁移建议：**
- 现有使用`pageWithJoin()`方法的代码应迁移到`pageWithVoMapping()`或`pageWithCondition()`
- 在VO类上添加`@VoMapping`注解配置多表关联
- 删除相关的`JoinPageDTO`配置代码

### **实际使用示例**

#### **ProductMultiTableVO配置**

```java
@Data
@EqualsAndHashCode(callSuper = true)
@VoMapping(
    table = "product",
    alias = "p",
    joins = {
        @VoMapping.Join(
            table = "brand", 
            alias = "b", 
            type = VoMapping.JoinType.LEFT,
            on = "p.brand_id = b.id"
        ),
        @VoMapping.Join(
            table = "category", 
            alias = "c", 
            type = VoMapping.JoinType.LEFT,
            on = "p.category_id = c.id"
        )
    },
    fields = {
        @VoMapping.Field(source = "p.product_name", target = "productName"),
        @VoMapping.Field(source = "b.brand_name", target = "brandName"),
        @VoMapping.Field(source = "c.category_name", target = "categoryName")
    }
)
public class ProductMultiTableVO extends BaseVO {
    private String productName;
    private String brandName;
    private String categoryName;
}
```

#### **Service层使用**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    // 推荐方式：使用@VoMapping注解
    PageResult<ProductMultiTableVO> pageProducts(ProductPageQueryDTO queryDTO);
    
    // 或者使用便捷方法
    PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
}
```

---

## ⚙️ **配置属性说明**

### **自动Repository配置**

在`application.yml`中配置包扫描路径：

```yaml
synapse:
  databases:
    auto-repository:
      enabled: true  # 启用自动Repository功能（默认true）
      base-packages:  # 扫描的包路径
        - com.indigo.repository
        - com.yourcompany.repository
        - com.example.repository
      debug: false   # 启用调试日志（默认false）
      bean-name-strategy: SIMPLE_NAME  # Bean名称生成策略
```

### **VO类配置**

```java
// 单表VO
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVO extends BaseVO {
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
}

// 多表VO
@Data
@EqualsAndHashCode(callSuper = true)
@VoMapping(
    table = "product",
    alias = "p",
    joins = {
        @VoMapping.Join(
            table = "brand", 
            alias = "b", 
            type = VoMapping.JoinType.LEFT,
            on = "p.brand_id = b.id"
        )
    },
    fields = {
        @VoMapping.Field(source = "p.product_name", target = "productName"),
        @VoMapping.Field(source = "b.brand_name", target = "brandName")
    }
)
public class ProductMultiTableVO extends BaseVO {
    private String productName;
    private String brandName;
}
```

### **DTO类配置**

```java
// 基础查询DTO
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageQueryDTO extends PageDTO {
    private String productName;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer status;
    
    // 排序配置
    private List<OrderBy> orderByList;
}

// 聚合查询DTO
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductAggregationPageDTO extends AggregationPageDTO {
    private String categoryId;
    private String brandId;
}
```

### **MyBatis-Plus 配置**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `log-impl` | String | `org.apache.ibatis.logging.stdout.StdOutImpl` | 日志实现类 |
| `map-underscore-to-camel-case` | boolean | `true` | 下划线转驼峰 |
| `cache-enabled` | boolean | `true` | 缓存启用 |
| `lazy-loading-enabled` | boolean | `true` | 延迟加载启用 |

### **动态数据源配置**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `primary` | String | `master1` | 主数据源名称 |
| `strict` | boolean | `false` | 是否启用严格模式 |
| `seata` | boolean | `false` | 是否启用Seata分布式事务 |
| `p6spy` | boolean | `false` | 是否启用P6Spy |

### **HikariCP 配置**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minimum-idle` | Integer | `5` | 最小空闲连接数 |
| `maximum-pool-size` | Integer | `15` | 最大连接池大小 |
| `idle-timeout` | Long | `30000` | 空闲超时时间(毫秒) |
| `max-lifetime` | Long | `1800000` | 最大生命周期(毫秒) |

---

## ⚡ **性能优化建议**

### **1. 使用VO映射**
- 避免内存转换，直接映射到VO对象
- 通过VoFieldSelector自动选择需要的字段

### **2. 启用缓存**
- 对于频繁查询的数据启用缓存
- 使用性能监控查询分析慢查询

### **3. 合理分页**
- 避免过大的分页大小
- 使用合适的索引

### **4. 监控性能**
- 定期使用性能监控查询
- 分析执行计划和性能评级

### **5. 数据库优化**
- 确保查询字段有合适的索引
- 优化SQL查询语句
- 合理使用连接池配置

---

## 🎯 **最佳实践**

### **1. Repository设计**
- 优先使用VO方法：所有查询方法都支持VO映射
- 使用便捷方法：`quickPage()`, `quickList()`, `quickGetOne()`
- 合理使用聚合查询：避免不必要的聚合计算

### **2. 配置管理**
- 使用`@AutoRepository`注解标记Repository接口
- 配置正确的包扫描路径
- 启用调试日志进行开发调试

### **3. 查询优化**
- 监控查询性能：定期使用性能监控查询
- 缓存策略：对热点数据启用缓存
- 错误处理：所有查询方法都有统一的异常处理

### **4. 代码规范**
- Repository接口使用`Repository`后缀
- VO类继承`BaseVO`
- DTO类继承`PageDTO`或`QueryDTO`
- 使用`@VoMapping`注解配置多表关联

---

## ❓ **常见问题**

### **Q1: Spring Bean找不到怎么办？**

**A:** 确保在`application.yml`中配置正确的包扫描路径：

```yaml
synapse:
  databases:
    auto-repository:
      base-packages:
        - com.indigo.repository  # 你的Repository包路径
```

### **Q2: 如何使用多表关联查询？**

**A:** 推荐使用`@VoMapping`注解方式：

```java
@VoMapping(
    table = "product",
    alias = "p",
    joins = {
        @VoMapping.Join(
            table = "brand", 
            alias = "b", 
            type = VoMapping.JoinType.LEFT,
            on = "p.brand_id = b.id"
        )
    }
)
public class ProductMultiTableVO extends BaseVO {
    // 字段定义
}
```

### **Q3: 如何优化查询性能？**

**A:** 
1. 使用VO映射避免内存转换
2. 启用缓存
3. 使用性能监控查询
4. 合理配置分页大小
5. 确保数据库索引

### **Q4: JoinPageDTO方式还能用吗？**

**A:** 可以，但已标记为过时。建议迁移到`@VoMapping`注解方式：

```java
// 旧方式（已过时）
@Deprecated(since = "1.0.0", forRemoval = true)
PageResult<ProductMultiTableVO> pageProductsWithJoin(JoinPageDTO joinPageDTO);

// 新方式（推荐）
PageResult<ProductMultiTableVO> pageProducts(ProductPageQueryDTO queryDTO);
```

### **Q5: 如何配置Bean名称生成策略？**

**A:** 在配置文件中设置：

```yaml
synapse:
  databases:
    auto-repository:
      bean-name-strategy: SIMPLE_NAME  # SIMPLE_NAME, FULL_NAME, CAMEL_CASE
```

---

## 📝 **注意事项**

1. **配置前缀变更**: 从 `synapse.databases` 变更为 `synapse.datasource`
2. **移除 enabled 属性**: 不再需要 `enabled: true` 配置
3. **向后兼容**: 仍然支持 `spring.datasource.dynamic` 配置格式
4. **类型安全**: 使用枚举类型确保配置的正确性
5. **BaseRepository是接口**: 需要使用`@AutoRepository`注解，不要使用`extends BaseRepositoryImpl`

---

## 🔄 **迁移指南**

如果你正在从旧版本迁移，请按照以下步骤操作：

1. 将配置前缀从 `synapse.databases` 改为 `synapse.datasource`
2. 移除 `enabled: true` 配置项
3. 更新代码中的配置类引用（如果直接使用配置类）
4. 将`extends BaseRepositoryImpl`改为使用`@AutoRepository`注解
5. 配置正确的包扫描路径
6. 测试配置是否正确加载

---

## 🚀 **总结**

现在`BaseRepository`完全支持`ProductMultiTableVO`示例中展示的`@VoMapping`注解方式！

**✅ 推荐使用：**
- `pageWithVoMapping()` - 专门支持@VoMapping注解的方法
- `pageWithCondition()` - 基础方法，也支持@VoMapping注解
- `quickPage()` - 便捷方法，也支持@VoMapping注解

**❌ 已过时的方法：**
- `pageWithJoin()` - 已标记为过时，将在未来版本中移除

**迁移指南：**
1. 将现有的`pageWithJoin(joinPageDTO, voClass)`调用替换为`pageWithVoMapping(pageDTO, voClass)`
2. 在VO类上添加`@VoMapping`注解配置多表关联
3. 删除相关的`JoinPageDTO`配置代码

这样就能完美匹配`ProductMultiTableVO`示例的使用方式了！🚀

---

## 🔒 **自动字段填充**

### **概述**

Synapse Framework 提供了强大的自动字段填充功能，支持审计字段的自动填充，包括创建时间、修改时间、用户信息、乐观锁版本号和逻辑删除标记。

### **实体类继承**

```java
@TableName("users")
public class Users extends AuditEntity<String> {
    private String account;
    private String password;
    private Boolean locked;
    private Boolean enabled;
    private Boolean expired;
    private LocalDateTime lastLoginTime;
    
    // 自动填充字段（继承自AuditEntity）：
    // - id: 主键（自动生成）
    // - createTime: 创建时间
    // - createUser: 创建人
    // - modifyTime: 修改时间
    // - modifyUser: 修改人
    // - revision: 乐观锁版本号（初始值1）
    // - deleted: 逻辑删除标记（初始值false）
}
```

### **字段说明**

| 字段 | 类型 | 说明 | 填充时机 | 默认值 |
|------|------|------|----------|--------|
| `id` | T | 主键 | 插入时 | 自动生成 |
| `createTime` | LocalDateTime | 创建时间 | 插入时 | 当前时间 |
| `createUser` | T | 创建人 | 插入时 | 当前用户ID |
| `modifyTime` | LocalDateTime | 修改时间 | 插入/更新时 | 当前时间 |
| `modifyUser` | T | 修改人 | 插入/更新时 | 当前用户ID |
| `revision` | Integer | 乐观锁版本号 | 插入时 | 1 |
| `deleted` | Boolean | 逻辑删除标记 | 插入时 | false |

### **配置要求**

确保在应用启动类中添加必要的包扫描：

```java
@SpringBootApplication(
    scanBasePackages = {"com.indigo.iam", "com.indigo.databases", "com.indigo.core"}
)
@MapperScan("com.indigo.iam.repository.mapper")
public class IAMApplication {
    public static void main(String[] args) {
        SpringApplication.run(IAMApplication.class, args);
    }
}
```

---

## ✅ **配置验证**

### **概述**

启动时自动验证数据源配置的完整性和连接性，确保应用能够正常启动。

### **验证内容**

1. **主数据源验证**：检查主数据源是否存在
2. **读写分离配置验证**：验证读写数据源配置
3. **连接池配置验证**：检查连接池参数合理性
4. **数据源连接性验证**：测试所有数据源的连接
5. **配置摘要输出**：显示完整的配置信息

### **启用调试日志**

```yaml
logging:
  level:
    com.indigo.databases: DEBUG
```

### **验证日志示例**

```
2025-09-22 16:28:49.569 [main] INFO  [DataSourceConfigurationValidator] - 开始验证数据源配置...
2025-09-22 16:28:49.570 [pool-6-thread-1] DEBUG [DataSourceHealthChecker] - DataSource [master1] is healthy
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] - ✅ 主数据源验证通过: [master1]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] - ✅ 数据源 [master1] 连接测试通过
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] - 📊 数据源配置摘要:
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] -    主数据源: [master1]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] -    总数据源数: [1]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] -    读写分离: [禁用]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] -    负载均衡策略: [ROUND_ROBIN]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] -    故障转移: [启用]
2025-09-22 16:28:49.570 [main] INFO  [DataSourceConfigurationValidator] - 数据源配置验证完成 ✅
```

---

## 🔧 **问题修复记录**

### **修复的问题**

#### **1. MyBatis绑定异常**
**问题**：`Invalid bound statement (not found): com.indigo.iam.repository.mapper.IamUserMapper.selectList`

**原因**：
- 重复的`@MapperScan`注解配置
- 错误的`SqlSessionFactory`配置

**解决方案**：
- 移除`MybatisPlusConfig`中的重复`@MapperScan`注解
- 使用`MybatisSqlSessionFactoryBean`替代`SqlSessionFactoryBean`
- 移除XML映射文件配置，使用MyBatis-Plus注解方式

#### **2. 数据源配置验证失败**
**问题**：`主数据源 [master1] 不存在`

**原因**：`DataSourceConfigurationValidator`依赖注入问题

**解决方案**：
- 修改构造函数参数，使用`DynamicRoutingDataSource`替代`Map<String, DataSource>`
- 更新所有相关方法调用

#### **3. 字段自动填充缺失**
**问题**：`revision`和`deleted`字段没有在插入时自动填充

**原因**：缺少`@TableField(fill = FieldFill.INSERT)`注解和填充逻辑

**解决方案**：
- 在`AuditEntity`中添加自动填充注解
- 在`MyMetaObjectHandler`中添加填充逻辑

#### **4. 编译错误**
**问题**：`找不到符号: 方法 setGlobalConfig`

**原因**：使用了错误的`SqlSessionFactoryBean`类

**解决方案**：
- 使用`MybatisSqlSessionFactoryBean`替代`SqlSessionFactoryBean`
- 添加正确的import语句

### **修复后的配置**

```java
@Bean
@Primary
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
    factoryBean.setPlugins(mybatisPlusInterceptor());
    factoryBean.setGlobalConfig(globalConfig());
    
    MybatisConfiguration configuration = new MybatisConfiguration();
    configuration.setMapUnderscoreToCamelCase(true);
    configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
    factoryBean.setConfiguration(configuration);
    
    return factoryBean.getObject();
}
```

---

## 📚 **相关文档**

- [BaseRepository 使用指南](./src/main/java/com/indigo/databases/repository/BaseRepository_README.md)
- [EnhancedQueryBuilder 使用指南](./EnhancedQueryBuilder_README.md)
- [@AutoRepository 使用指南](./src/main/java/com/indigo/databases/repository/AutoRepository_使用指南.md)
- [多表查询方式对比](./src/main/java/com/indigo/databases/repository/BaseRepository_多表查询方式对比.md)

---

## 🤝 **贡献**

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 📄 **许可证**

本项目采用 Apache License 2.0 许可证。
