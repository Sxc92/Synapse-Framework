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
├── BusinessException (业务异常)
├── IAMException (身份认证异常)
├── I18nException (国际化异常)
├── SecurityException (安全异常)
├── AssertException (断言异常)
└── RateLimitException (限流异常)
```

#### **错误码体系**

- **`ErrorCode`**: 错误码接口
- **`BaseErrCode`**: 基础错误码
- **`StandardErrorCode`**: 标准错误码枚举

#### **异常处理特性**

- **动态异常创建**: 通过反射机制动态创建异常
- **自动国际化**: I18nException自动使用当前语言环境
- **构造函数缓存**: 提高异常创建性能
- **全局异常处理**: 自动捕获和处理各种异常

#### **错误代码分类**

```java
public enum ErrorCode {
    // 系统级错误码 (SYS)
    SYSTEM_ERROR("SYS001", "系统内部错误"),
    PARAM_ERROR("SYS002", "参数错误"),
    UNAUTHORIZED("SYS003", "未授权"),
    FORBIDDEN("SYS004", "禁止访问"),
    NOT_FOUND("SYS005", "资源不存在"),
    
    // 业务级错误码 (BUS)
    BUSINESS_ERROR("BUS001", "业务处理错误"),
    DATA_NOT_FOUND("BUS002", "数据不存在"),
    DATA_ALREADY_EXISTS("BUS003", "数据已存在"),
    DATA_INVALID("BUS004", "数据无效"),
    
    // 安全认证相关错误码 (SEC)
    SECURITY_ERROR("SEC001", "安全错误"),
    NOT_LOGIN("SEC002", "未登录"),
    LOGIN_FAILED("SEC003", "登录失败"),
    TOKEN_INVALID("SEC005", "令牌无效"),
    PERMISSION_DENIED("SEC008", "权限不足"),
    
    // IAM相关错误码 (IAM)
    IAM_ERROR("IAM001", "身份认证错误"),
    USER_NOT_FOUND("IAM002", "用户不存在"),
    USER_ALREADY_EXISTS("IAM003", "用户已存在"),
    USER_INVALID("IAM004", "用户无效"),
    
    // 工作流相关错误码 (WF)
    WORKFLOW_ERROR("WF001", "工作流错误"),
    WORKFLOW_NOT_FOUND("WF002", "工作流不存在"),
    
    // 审计相关错误码 (AUD)
    AUDIT_ERROR("AUD001", "审计错误"),
    AUDIT_NOT_FOUND("AUD002", "审计记录不存在");
}
```

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

#### **语言环境处理**

- **`LocaleContext`**: 语言环境上下文
- **`MessageResolver`**: 消息解析器

#### **Gateway架构下的语言环境处理**

在Spring Gateway架构中，语言环境处理方案已经简化，专注于从Gateway传递的请求头获取语言环境信息：

1. **LocaleContextHolder** - 语言环境上下文持有者（简化版）
2. **I18nConfig** - 国际化配置（移除拦截器）

#### **语言环境获取优先级**

```
1. ThreadLocal (最高优先级)
   ↓
2. Gateway传递的请求头 (主要方式)
   - X-Locale
   - X-Language
   - Accept-Language
   ↓
3. Spring LocaleResolver (备用方式)
   ↓
4. 系统默认语言环境 (最低优先级)
```

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

#### **基本异常创建**

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

#### **动态异常创建**

```java
// 创建业务异常
BusinessException businessEx = ExceptionUtils.createException(
    BusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "用户ID", "12345"
);

// 创建IAM异常
IAMException iamEx = ExceptionUtils.createException(
    IAMException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户名", "admin"
);

// 创建I18n异常（自动使用当前语言环境）
I18nException i18nEx = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);
```

#### **带原因的异常创建**

```java
try {
    // 一些可能抛出异常的操作
    someRiskyOperation();
} catch (Exception e) {
    // 包装原始异常
    BusinessException wrappedEx = ExceptionUtils.createException(
        BusinessException.class, 
        ErrorCode.OPERATION_FAILED, 
        e,  // 原始异常作为原因
        "操作参数", "value"
    );
    throw wrappedEx;
}
```

#### **条件异常抛出**

```java
// 如果条件为真，抛出指定类型的异常
ExceptionUtils.throwIf(
    user == null, 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);

// 带原因的条件异常抛出
ExceptionUtils.throwIf(
    connection == null, 
    IAMException.class, 
    ErrorCode.SYSTEM_ERROR, 
    connectionException, 
    "数据库连接失败"
);
```

#### **空值检查**

```java
// 如果对象为null，抛出指定类型的异常
User user = ExceptionUtils.requireNonNull(
    userService.findById(userId), 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);
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

#### **自动语言环境处理**

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

#### **Gateway传递语言环境**

Gateway可以通过以下请求头传递语言环境：

```http
# 方式1：自定义请求头（推荐）
X-Locale: zh_CN

# 方式2：语言环境请求头
X-Language: zh_CN

# 方式3：标准Accept-Language头
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
```

#### **手动设置语言环境**

```java
// 手动设置语言环境
LocaleContextHolder.setCurrentLocale(Locale.SIMPLIFIED_CHINESE);

// 获取当前语言环境
Locale currentLocale = LocaleContextHolder.getCurrentLocale();

// 清理语言环境
LocaleContextHolder.clearCurrentLocale();
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

### **4. 简化国际化配置**

```java
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class I18nConfig implements WebMvcConfigurer {
    
    // 语言环境解析器
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }
    
    // 消息源配置
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setCacheSeconds(-1);
        return messageSource;
    }
    
    // 注册Spring标准语言切换拦截器（可选）
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/actuator/**", "/static/**", "/favicon.ico");
    }
}
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
- 反射创建器会缓存构造函数，重复创建相同类型的异常性能很好

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

// 推荐：使用预定义的错误代码
ExceptionUtils.throwIf(condition, BusinessException.class, ErrorCode.BUSINESS_ERROR);

// 避免：硬编码错误代码
ExceptionUtils.throwIf(condition, BusinessException.class, "CUSTOM001");
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

### **4. 自定义异常**

```java
// 创建自定义异常类
public class CustomBusinessException extends BusinessException {
    public CustomBusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
    
    public CustomBusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}

// 使用自定义异常
CustomBusinessException ex = ExceptionUtils.createException(
    CustomBusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "自定义业务错误"
);
```

### **5. 国际化异常示例**

```java
// 请求头：X-Locale: zh_CN
I18nException ex = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);
// 异常消息：权限不足：缺少功能权限 - 功能: 用户管理

// 请求头：X-Locale: en_US
I18nException ex = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "function", "user management"
);
// 异常消息：Permission denied: missing function permission - function: user management
```

## 🔒 **安全特性**

### **1. 异常信息安全**

- **敏感信息过滤**：异常消息中不包含敏感信息
- **错误码标准化**：统一的错误码体系，避免信息泄露
- **异常链保护**：异常链中的敏感信息自动脱敏

### **2. 异常访问控制**

- **权限验证**：异常创建和处理的权限控制
- **审计日志**：异常操作的审计日志记录
- **访问限制**：异常信息的访问限制

### **3. ThreadLocal管理**

- **自动清理**：拦截器自动清理ThreadLocal，避免内存泄漏
- **异常安全**：即使发生异常也会清理ThreadLocal

### **4. 语言环境验证**

- **格式验证**：验证语言环境格式是否正确
- **支持检查**：检查语言环境是否被支持
- **异常处理**：解析失败时使用默认语言环境

## 🐛 **常见问题**

### **1. 异常创建失败**

**问题**：反射创建异常时失败
**解决方案**：
- 确保异常类有正确的构造函数
- 检查异常类是否继承自BaseException
- 验证ErrorCode是否正确

### **2. 国际化异常不生效**

**问题**：I18nException没有使用正确的语言环境
**解决方案**：
- 检查语言环境获取逻辑
- 验证MessageUtils配置
- 确认语言包是否完整

### **3. 语言环境不生效**

**问题**：设置的语言环境没有生效
**解决方案**：
- 检查拦截器是否正确注册
- 检查请求头格式是否正确
- 检查ThreadLocal是否正确设置

### **4. 异常处理器不生效**

**问题**：自定义异常处理器没有被调用
**解决方案**：
- 检查@ExceptionHandler注解
- 验证异常类型匹配
- 确认处理器是否被Spring管理

### **5. 性能问题**

**问题**：异常创建性能不佳
**解决方案**：
- 使用构造函数缓存
- 避免频繁创建异常
- 考虑异常对象池

### **6. 内存泄漏**

**问题**：ThreadLocal没有清理导致内存泄漏
**解决方案**：
- 确保拦截器的afterCompletion方法被调用
- 手动调用clearCurrentLocale()方法
- 检查异常处理逻辑

## 🔄 **版本历史**

- **v1.0.0**: 初始版本，提供基础功能
- **v1.1.0**: 增加智能VO映射功能
- **v1.2.0**: 增加多表关联查询支持
- **v1.3.0**: 异常处理模块重构，删除8个不必要的异常类，统一异常设计

## 📝 **注意事项**

1. **依赖管理**: 该模块不依赖业务模块，保持独立性
2. **版本兼容**: 遵循语义化版本控制
3. **性能考虑**: 工具类经过性能优化，适合生产环境
4. **扩展性**: 支持自定义扩展和配置
5. **异常类型选择**: 
   - **BusinessException**：业务逻辑错误
   - **IAMException**：系统级错误
   - **I18nException**：需要国际化的错误
   - **SecurityException**：安全相关错误
   - **AssertException**：断言失败
6. **异常链保持**: 保持异常链，便于问题追踪
7. **性能考虑**: 反射创建器会缓存构造函数，重复创建相同类型的异常性能很好，但首次创建会有反射开销，建议在应用启动时预热

## 🤝 **贡献指南**

1. 遵循代码规范
2. 添加必要的测试用例
3. 更新相关文档
4. 确保向后兼容性

---

**Synapse Core** - 为您的应用提供坚实的技术基础！🚀