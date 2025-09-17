# 基于权限编码的灵活权限校验系统使用指南

## 概述

本系统基于权限编码实现了灵活的权限校验机制，支持：
- 基于权限编码的接口级权限校验
- 基于部门职级的数据权限过滤
- 动态SQL表达式参数替换
- 通配符权限支持

## 核心组件

### 1. 权限编码常量 (PermissionCode)

```java
// 用户管理权限
PermissionCode.User.CREATE     // "user:create"
PermissionCode.User.READ       // "user:read"
PermissionCode.User.UPDATE     // "user:update"
PermissionCode.User.DELETE     // "user:delete"
PermissionCode.User.ALL        // "user:*"

// 订单管理权限
PermissionCode.Order.APPROVE   // "order:approve"
PermissionCode.Order.REJECT    // "order:reject"
PermissionCode.Order.ALL       // "order:*"
```

### 2. 权限校验服务 (PermissionValidationService)

```java
@Autowired
private PermissionValidationService permissionValidationService;

// 检查权限
boolean hasPermission = permissionValidationService.hasPermission(PermissionCode.User.READ);

// 获取数据权限SQL
String dataScopeSql = permissionValidationService.getDataScopeSql("user");

// 批量权限检查
Map<String, Boolean> results = permissionValidationService.batchCheckPermissions(
    Arrays.asList(PermissionCode.User.READ, PermissionCode.User.UPDATE)
);
```

### 3. 扩展的用户上下文 (UserContext)

```java
UserContext user = UserContext.getCurrentUser();

// 基础信息
String userId = user.getUserId();
String deptId = user.getDeptId();
String positionId = user.getPositionId();

// 部门职级信息
List<UserContext.UserDeptPositionInfo> deptPositions = user.getDeptPositions();
```

## 使用示例

### 1. Controller层权限校验

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private PermissionValidationService permissionValidationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户列表查询
     * 权限校验：user:read
     * 数据权限：根据用户部门职级自动过滤
     */
    @GetMapping("/list")
    public Result<List<UserDTO>> getUserList(@RequestParam(required = false) String keyword) {
        // 1. 权限校验
        if (!permissionValidationService.hasPermission(PermissionCode.User.READ)) {
            return Result.error("没有用户查看权限");
        }
        
        // 2. 构建查询条件
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        
        // 3. 添加数据权限SQL条件
        String dataScopeSql = permissionValidationService.getDataScopeSql("user");
        wrapper.apply(dataScopeSql);
        
        // 4. 添加业务查询条件
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like("username", keyword)
                .or().like("real_name", keyword)
                .or().like("email", keyword));
        }
        
        // 5. 执行查询
        List<User> users = userService.list(wrapper);
        return Result.success(users);
    }
    
    /**
     * 创建用户
     * 权限校验：user:create
     */
    @PostMapping("/create")
    public Result<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        // 权限校验
        if (!permissionValidationService.hasPermission(PermissionCode.User.CREATE)) {
            return Result.error("没有用户创建权限");
        }
        
        User user = userService.createUser(request);
        return Result.success(user);
    }
    
    /**
     * 用户导出
     * 权限校验：user:export
     */
    @GetMapping("/export")
    public Result<String> exportUsers() {
        // 权限校验
        if (!permissionValidationService.hasPermission(PermissionCode.User.EXPORT)) {
            return Result.error("没有用户导出权限");
        }
        
        // 构建导出查询条件（包含数据权限）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        String dataScopeSql = permissionValidationService.getDataScopeSql("user");
        wrapper.apply(dataScopeSql);
        
        List<User> users = userService.list(wrapper);
        String exportUrl = userService.exportUsers(users);
        
        return Result.success(exportUrl);
    }
}
```

### 2. 订单模块示例

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    @Autowired
    private PermissionValidationService permissionValidationService;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 订单列表查询
     * 权限校验：order:read
     * 数据权限：根据用户部门职级自动过滤
     */
    @GetMapping("/list")
    public Result<List<OrderDTO>> getOrderList(@RequestParam(required = false) String status) {
        // 权限校验
        if (!permissionValidationService.hasPermission(PermissionCode.Order.READ)) {
            return Result.error("没有订单查看权限");
        }
        
        // 构建查询条件
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        
        // 添加数据权限SQL条件
        String dataScopeSql = permissionValidationService.getDataScopeSql("order");
        wrapper.apply(dataScopeSql);
        
        // 添加业务查询条件
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq("status", status);
        }
        
        List<Order> orders = orderService.list(wrapper);
        return Result.success(orders);
    }
    
    /**
     * 订单审批
     * 权限校验：order:approve
     */
    @PostMapping("/{orderId}/approve")
    public Result<Void> approveOrder(@PathVariable String orderId, 
                                   @RequestBody ApproveOrderRequest request) {
        // 权限校验
        if (!permissionValidationService.hasPermission(PermissionCode.Order.APPROVE)) {
            return Result.error("没有订单审批权限");
        }
        
        orderService.approveOrder(orderId, request);
        return Result.success();
    }
}
```

### 3. 权限规则配置

#### 数据库权限规则配置示例

```sql
-- 权限规则配置示例
INSERT INTO iam_data_permission_rule (
    rule_name, permission_code, subject_type, subject_id, 
    resource_type, data_scope_type, data_scope_expression, priority, enabled
) VALUES 
-- 技术部高级工程师：所有用户权限
('技术部高级工程师-用户权限', 'user:*', 'DEPT_POSITION', 'dept001:pos001', 
 'user', 'DEPARTMENT_AND_BELOW', NULL, 1, 1),

-- 技术部普通工程师：用户查看权限
('技术部普通工程师-用户查看', 'user:read', 'DEPT_POSITION', 'dept001:pos002', 
 'user', 'DEPARTMENT', NULL, 2, 1),

-- 人事部经理：所有用户权限
('人事部经理-用户权限', 'user:*', 'DEPT_POSITION', 'dept002:pos001', 
 'user', 'ALL', NULL, 1, 1),

-- 财务部经理：订单审批权限（自定义SQL）
('财务部经理-订单审批', 'order:approve', 'DEPT_POSITION', 'dept003:pos001', 
 'order', 'CUSTOM', 'dept_id = #{deptId} OR create_user_id = #{userId}', 1, 1),

-- 销售部：订单权限（自定义SQL）
('销售部-订单权限', 'order:*', 'DEPT_POSITION', 'dept004:pos001', 
 'order', 'CUSTOM', 'salesman_id = #{userId} OR dept_id = #{deptId}', 1, 1);
```

### 4. 动态SQL表达式示例

#### 支持的动态参数

```sql
-- 基础用户信息
#{userId}        -- 当前用户ID
#{username}      -- 当前用户名
#{deptId}        -- 当前用户部门ID
#{positionId}    -- 当前用户职级ID
#{positionLevel} -- 当前用户职级等级

-- 部门职级组合信息
#{deptIds}       -- 用户所有部门ID列表
#{positionIds}   -- 用户所有职级ID列表
#{deptPositionIds} -- 用户所有部门职级组合ID列表

-- 角色和权限信息
#{roles}         -- 用户角色列表
#{permissions}   -- 用户权限列表
```

#### SQL表达式示例

```sql
-- 1. 基础数据权限
"dept_id = #{deptId}"
"position_id = #{positionId}"
"create_user_id = #{userId}"

-- 2. 复杂数据权限
"dept_id IN (#{deptIds})"
"position_id IN (#{positionIds})"
"dept_position_id IN (#{deptPositionIds})"

-- 3. 自定义业务权限
"salesman_id = #{userId} OR dept_id = #{deptId}"
"approver_id = #{userId} OR position_level >= #{positionLevel}"
"(dept_id = #{deptId} AND position_level >= 3) OR create_user_id = #{userId}"

-- 4. 多条件组合
"status = 'ACTIVE' AND (dept_id = #{deptId} OR create_user_id = #{userId})"
```

### 5. 认证流程集成

```java
@Service
public class IAMAuthenticationService {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private DeptPositionPermissionService deptPositionPermissionService;
    
    public Result<AuthResponse> login(String username, String password) {
        // 1. 验证用户名密码
        User user = userService.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        
        // 2. 获取用户角色和权限
        List<String> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());
        
        // 3. 获取用户部门职级信息
        List<AuthRequest.UserDeptPositionInfo> deptPositions = deptPositionPermissionService
            .getUserDeptPositionInfos(user.getId());
        
        // 4. 构建认证请求
        AuthRequest request = AuthRequest.builder()
            .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
            .usernamePasswordAuth(UsernamePasswordAuth.builder()
                .username(username)
                .password(password)
                .build())
            .userId(user.getId().toString())
            .roles(roles)
            .permissions(permissions)
            .deptPositions(deptPositions)  // 新增部门职级信息
            .build();
        
        // 5. 调用认证服务
        return authenticationService.authenticate(request);
    }
}
```

## 配置说明

### 1. 应用配置

```yaml
synapse:
  security:
    enabled: true
    mode: STRICT
    dept-position-permission:
      enabled: true
      cache-enabled: true
      cache-ttl: 3600
```

### 2. 权限规则配置

```yaml
# 权限规则配置示例
dept-position-permissions:
  rules:
    - dept-position-id: "dept001:pos001"  # 技术部:高级工程师
      resource-type: "user"
      permission-code: "user:*"
      data-scope-type: "DEPARTMENT_AND_BELOW"
      priority: 1
      
    - dept-position-id: "dept001:pos002"  # 技术部:普通工程师
      resource-type: "user"
      permission-code: "user:read"
      data-scope-type: "DEPARTMENT"
      priority: 2
      
    - dept-position-id: "dept002:pos001"  # 人事部:经理
      resource-type: "user"
      permission-code: "user:*"
      data-scope-type: "ALL"
      priority: 1
```

## 优势特点

### 1. 灵活性
- ✅ **无限权限类型**：基于权限编码，支持任意权限定义
- ✅ **通配符支持**：支持 `user:*` 等通配符权限
- ✅ **动态SQL**：支持动态参数替换的SQL表达式
- ✅ **细粒度控制**：可以精确控制到具体操作

### 2. 易用性
- ✅ **接口级校验**：每个接口独立权限校验
- ✅ **自动数据过滤**：根据用户信息自动拼接SQL
- ✅ **统一权限管理**：所有权限通过编码统一管理
- ✅ **易于扩展**：新增权限只需添加编码和规则

### 3. 性能优化
- ✅ **缓存支持**：权限规则和用户权限缓存
- ✅ **SQL优化**：直接拼接SQL条件，避免多次查询
- ✅ **批量校验**：支持批量权限校验
- ✅ **懒加载**：按需加载权限规则

## 注意事项

1. **权限编码规范**：建议使用 `模块:资源:操作` 的格式
2. **SQL安全**：动态SQL表达式需要防止SQL注入
3. **缓存策略**：权限规则变更时需要清理相关缓存
4. **性能考虑**：复杂的数据权限规则可能影响查询性能
5. **测试覆盖**：权限校验逻辑需要充分的单元测试

## 扩展建议

1. **权限审计**：记录权限使用情况，便于审计
2. **权限继承**：支持权限的继承和传递
3. **临时权限**：支持临时权限和权限过期
4. **权限委托**：支持权限的委托和转移
5. **权限分析**：提供权限使用情况的分析报告
