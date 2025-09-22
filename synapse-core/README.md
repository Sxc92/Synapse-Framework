# Synapse Core 模块文档

## 📋 **模块概述**

`synapse-core` 是 Synapse 框架的核心模块，提供了基础的数据结构、工具类、异常处理、国际化支持等核心功能。该模块不依赖任何业务逻辑，为整个框架提供基础支撑。

## 🏗️ **模块结构**

```
synapse-core/
├── config/                 # 配置类
│   ├── serialization/      # 序列化配置
│   └── ThreadPoolConfig    # 线程池配置
├── constants/              # 常量定义
├── context/                # 上下文管理
├── entity/                 # 实体类
│   ├── dto/               # 数据传输对象
│   ├── result/            # 结果封装类
│   └── vo/                # 视图对象
├── exception/              # 异常处理
├── i18n/                  # 国际化支持
└── utils/                 # 工具类
```

## 🚀 **核心功能**

### 1. **实体类体系 (Entity)**

#### **基础实体类**

- **`BaseVO`**: 视图对象基类，包含通用字段
- **`Result<T>`**: 统一响应结果封装
- **`TreeNode<T>`**: 树形结构节点

#### **分页结果类**

- **`PageResult<T>`**: 基础分页结果
- **`AggregationPageResult<T>`**: 聚合查询分页结果
- **`PerformancePageResult<T>`**: 性能监控分页结果
- **`EnhancedPageResult<T>`**: 增强分页结果（组合功能）

#### **查询DTO**

- **`QueryDTO`**: 基础查询DTO
- **`PageDTO`**: 分页查询DTO
- **各种专用查询DTO**: 聚合、性能、复杂查询等

### 2. **异常处理体系 (Exception)**

#### **异常类层次**

```java
BaseException (基础异常)
├── SynapseException (框架异常)
└── Ex (业务异常)
```

#### **错误码体系**

- **`ErrorCode`**: 错误码接口
- **`BaseErrCode`**: 基础错误码
- **`StandardErrorCode`**: 标准错误码枚举

#### **全局异常处理**

- **`WebMvcGlobalExceptionHandler`**: Web MVC全局异常处理器

### 3. **工具类体系 (Utils)**

#### **集合工具**

- **`CollectionUtils`**: 集合操作工具
- **`MapUtils`**: Map操作工具

#### **日期时间工具**

- **`DateTimeUtils`**: 日期时间处理工具
- **`ZoneUtils`**: 时区处理工具

#### **JSON工具**

- **`JsonUtils`**: JSON序列化/反序列化工具

#### **其他工具**

- **`AssertUtils`**: 断言工具
- **`SpringUtils`**: Spring上下文工具
- **`ThreadUtils`**: 线程池工具
- **`TreeUtil`**: 树形结构工具
- **`UserContextHolder`**: 用户上下文工具

### 4. **上下文管理 (Context)**

#### **用户上下文**

- **`UserContext`**: 用户上下文信息
- **`UserContextHolder`**: 用户上下文持有者

### 5. **国际化支持 (I18n)**

- **`LocaleContext`**: 语言环境上下文
- **`MessageResolver`**: 消息解析器

### 6. **配置支持 (Config)**

#### **序列化配置**

- **`JacksonConfig`**: Jackson序列化配置
- **`CustomSerializers`**: 自定义序列化器

#### **线程池配置**

- **`ThreadPoolConfig`**: 线程池配置

## 📖 **使用指南**

### **1. 统一响应结果**

```java
@RestController
public class ProductController {
    
    @GetMapping("/products/{id}")
    public Result<ProductVO> getProduct(@PathVariable Long id) {
        ProductVO product = productService.getById(id);
        return Result.success(product);
    }
    
    @PostMapping("/products")
    public Result<ProductVO> createProduct(@RequestBody ProductCreateDTO dto) {
        ProductVO product = productService.create(dto);
        return Result.success(product);
    }
}
```

### **2. 分页查询**

```java
@Service
public class ProductService {
    
    public PageResult<ProductVO> pageProducts(PageDTO pageDTO) {
        // 执行分页查询
        List<ProductVO> records = productMapper.selectPage(pageDTO);
        Long total = productMapper.count(pageDTO);
        
        return PageResult.of(records, total, pageDTO.getPageNo(), pageDTO.getPageSize());
    }
}
```

### **3. 异常处理**

```java
@Service
public class ProductService {
    
    public ProductVO getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw Ex.business("产品不存在", id);
        }
        return BeanUtils.copyProperties(product, ProductVO.class);
    }
}
```

### **4. 用户上下文**

```java
@Service
public class ProductService {
    
    public void createProduct(ProductCreateDTO dto) {
        // 获取当前用户信息
        UserContext userContext = UserContextHolder.getCurrentUser();
        Long userId = userContext.getUserId();
        String tenantId = userContext.getTenantId();
        
        // 设置创建信息
        Product product = new Product();
        product.setCreateBy(userId);
        product.setTenantId(tenantId);
        // ... 其他业务逻辑
    }
}
```

### **5. 树形结构处理**

```java
@Service
public class CategoryService {
    
    public List<TreeNode<CategoryVO>> buildCategoryTree(List<CategoryVO> categories) {
        return TreeUtil.buildTree(categories, 
            CategoryVO::getId, 
            CategoryVO::getParentId, 
            CategoryVO::getChildren);
    }
}
```

### **6. 国际化消息**

```java
@Service
public class ProductService {
    
    @Autowired
    private MessageResolver messageResolver;
    
    public void validateProduct(ProductCreateDTO dto) {
        if (StringUtils.isBlank(dto.getName())) {
            String message = messageResolver.getMessage("product.name.required");
            throw Ex.business(message);
        }
    }
}
```

## 🔧 **配置说明**

### **1. Jackson序列化配置**

```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

### **2. 线程池配置**

```yaml
synapse:
  thread-pool:
    core-size: 10
    max-size: 50
    queue-capacity: 1000
    keep-alive-time: 60
```

### **3. 国际化配置**

```yaml
spring:
  messages:
    basename: i18n/messages
    encoding: UTF-8
    cache-duration: 3600
```

## 📊 **性能特性**

### **1. 分页结果优化**

- 支持多种分页结果类型
- 自动计算总页数、是否有下一页等
- 支持聚合查询和性能监控

### **2. 工具类优化**

- 集合操作工具提供高性能实现
- 日期时间工具支持多时区
- JSON工具支持自定义序列化器

### **3. 异常处理优化**

- 全局异常处理减少重复代码
- 错误码体系便于问题定位
- 支持国际化错误消息

## 🎯 **最佳实践**

### **1. 实体类设计**

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVO extends BaseVO {
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    
    // 业务方法
    public boolean isInStock() {
        return stock != null && stock > 0;
    }
    
    public String getStatusText() {
        return status == 1 ? "启用" : "禁用";
    }
}
```

### **2. 异常处理**

```java
// 业务异常
throw Ex.business("产品名称不能为空");

// 参数异常
throw Ex.param("产品ID不能为空");

// 系统异常
throw Ex.system("数据库连接失败");
```

### **3. 工具类使用**

```java
// 集合操作
List<String> names = CollectionUtils.extractToList(products, Product::getName);

// 日期处理
String dateStr = DateTimeUtils.format(LocalDateTime.now(), "yyyy-MM-dd");

// JSON处理
String json = JsonUtils.toJson(product);
ProductVO product = JsonUtils.toBean(json, ProductVO.class);
```

## 🔄 **版本历史**

- **v1.0.0**: 初始版本，提供基础功能
- **v1.1.0**: 增加智能VO映射功能
- **v1.2.0**: 增加多表关联查询支持

## 📝 **注意事项**

1. **依赖管理**: 该模块不依赖业务模块，保持独立性
2. **版本兼容**: 遵循语义化版本控制
3. **性能考虑**: 工具类经过性能优化，适合生产环境
4. **扩展性**: 支持自定义扩展和配置

## 🤝 **贡献指南**

1. 遵循代码规范
2. 添加必要的测试用例
3. 更新相关文档
4. 确保向后兼容性

---

**Synapse Core** - 为您的应用提供坚实的技术基础！🚀
