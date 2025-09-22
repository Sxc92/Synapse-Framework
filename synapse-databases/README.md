# Synapse Framework - 数据库模块

## 📋 **概述**

Synapse Framework 数据库模块是一个集成了 MyBatis-Plus 和动态数据源的强大数据库解决方案。它提供了灵活的配置选项，支持多种数据库类型和连接池，并且兼容标准的 Spring Boot 配置格式。

> 📚 **完整文档**: 查看 [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md) 获取详细的使用指南和API文档。

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

## 🚀 **快速开始**

### **1. 基础配置**

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
    dynamic-data-source:
      primary: master
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

### **2. 使用BaseRepository**

```java
@AutoRepository
public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
    
    /**
     * 基础分页查询
     */
    PageResult<ProductVO> pageProducts(ProductPageQueryDTO queryDTO);
    
    /**
     * 多表关联查询 - 基于@VoMapping注解
     */
    PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
}
```

### **3. Service层使用**

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public PageResult<ProductVO> getProducts(ProductPageQueryDTO queryDTO) {
        return productRepository.pageProducts(queryDTO);
    }
}
```

### **4. 自动Repository配置**

```yaml
synapse:
  databases:
    sql-annotation:
      enabled: true
      base-packages:
        - com.indigo.repository
        - com.yourcompany.repository
      debug: false
      bean-name-strategy: SIMPLE_NAME
```

### **5. 审计字段自动填充**

实体类继承 `AuditEntity` 即可自动填充审计字段：

```java
@TableName("users")
public class Users extends AuditEntity<String> {
    private String account;
    private String password;
    // 自动填充字段：
    // - id: 主键（自动生成）
    // - createTime: 创建时间
    // - createUser: 创建人
    // - modifyTime: 修改时间
    // - modifyUser: 修改人
    // - revision: 乐观锁版本号（初始值1）
    // - deleted: 逻辑删除标记（初始值false）
}
```

### **6. 配置验证**

启动时自动验证数据源配置：

```yaml
# 启用调试日志查看验证过程
logging:
  level:
    com.indigo.databases: DEBUG
```

## 📚 **更多信息**

- **完整文档**: [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md)
- **BaseRepository**: 强大的Repository接口，支持VO映射、多表关联查询
- **EnhancedQueryBuilder**: 增强查询构建器，支持聚合查询、性能监控
- **@AutoRepository**: 自动Repository注解，无需手动实现
- **多表查询**: 支持@VoMapping注解配置的多表关联查询

## 🤝 **贡献**

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 📄 **许可证**

本项目采用 Apache License 2.0 许可证。 