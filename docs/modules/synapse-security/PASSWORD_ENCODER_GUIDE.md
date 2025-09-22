# 密码加密工具使用指南

## 概述

`PasswordEncoderUtils` 是 Synapse Security 模块提供的密码加密工具类，支持多种密码加密算法和验证方法，为系统提供安全的密码管理功能。

## 主要特性

- 🔒 **多种加密算法**: 支持BCrypt、SCrypt、MD5、SHA256、PBKDF2
- 🔍 **自动检测**: 自动检测密码加密算法类型
- 💪 **密码强度**: 检查密码强度等级
- 🎲 **随机生成**: 生成随机密码和盐
- 🛡️ **安全验证**: 提供安全的密码验证方法
- 🔧 **Spring集成**: 与Spring Security无缝集成

## 快速开始

### 1. 依赖注入

```java
@Service
public class UserService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    // 业务方法...
}
```

### 2. 基础使用

```java
@Service
public class PasswordService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    /**
     * 加密密码（使用默认BCrypt）
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoderUtils.encode(rawPassword);
    }
    
    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoderUtils.matches(rawPassword, encodedPassword);
    }
}
```

## 加密算法详解

### 1. BCrypt（推荐）

BCrypt是最推荐的密码加密算法，具有以下特点：
- ✅ 自适应哈希算法
- ✅ 内置盐值生成
- ✅ 计算成本可调
- ✅ 抗彩虹表攻击

```java
// 使用BCrypt加密
String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.BCRYPT);

// 验证密码
boolean isValid = passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.BCRYPT);
```

### 2. SCrypt

SCrypt是一种内存密集型密码哈希函数：
- ✅ 抗ASIC攻击
- ✅ 内存硬函数
- ✅ 适合高安全要求场景

```java
// 使用SCrypt加密
String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SCRYPT);

// 验证密码
boolean isValid = passwordEncoderUtils.matches(rawPassword, encodedPassword, PasswordEncoderUtils.EncoderType.SCRYPT);
```

### 3. MD5（不推荐）

MD5算法已不推荐用于密码加密，仅用于兼容旧系统：
- ❌ 已被破解
- ❌ 容易受到彩虹表攻击
- ⚠️ 仅用于兼容

```java
// 使用MD5加密（不推荐）
String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.MD5);
```

### 4. SHA256

SHA256提供更好的安全性：
- ✅ 比MD5更安全
- ✅ 抗碰撞攻击
- ⚠️ 仍需要加盐

```java
// 使用SHA256加密
String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.SHA256);
```

### 5. PBKDF2

PBKDF2是基于密码的密钥派生函数：
- ✅ 可配置迭代次数
- ✅ 抗暴力破解
- ✅ 标准化算法

```java
// 使用PBKDF2加密
String encodedPassword = passwordEncoderUtils.encode(rawPassword, PasswordEncoderUtils.EncoderType.PBKDF2);
```

## 高级功能

### 1. 自动检测算法

```java
@Service
public class AutoDetectionService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    /**
     * 自动检测并验证密码
     */
    public boolean validatePasswordAuto(String rawPassword, String encodedPassword) {
        return passwordEncoderUtils.matchesAuto(rawPassword, encodedPassword);
    }
    
    /**
     * 检测密码加密算法类型
     */
    public PasswordEncoderUtils.EncoderType detectAlgorithm(String encodedPassword) {
        return passwordEncoderUtils.detectEncoderType(encodedPassword);
    }
}
```

### 2. 密码强度检查

```java
@Service
public class PasswordStrengthService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    /**
     * 检查密码强度
     */
    public PasswordEncoderUtils.PasswordStrength checkStrength(String password) {
        return passwordEncoderUtils.checkPasswordStrength(password);
    }
    
    /**
     * 验证密码强度
     */
    public boolean isPasswordStrongEnough(String password) {
        PasswordEncoderUtils.PasswordStrength strength = passwordEncoderUtils.checkPasswordStrength(password);
        return strength != PasswordEncoderUtils.PasswordStrength.WEAK;
    }
}
```

### 3. 随机密码生成

```java
@Service
public class RandomPasswordService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    /**
     * 生成随机密码
     */
    public String generateRandomPassword(int length) {
        return passwordEncoderUtils.generateRandomPassword(length);
    }
    
    /**
     * 生成随机盐
     */
    public String generateSalt(int length) {
        return passwordEncoderUtils.generateSalt(length);
    }
}
```

## 实际应用示例

### 1. 用户注册

```java
@Service
public class UserRegistrationService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    public boolean registerUser(String username, String password, String email) {
        try {
            // 检查密码强度
            PasswordEncoderUtils.PasswordStrength strength = passwordEncoderUtils.checkPasswordStrength(password);
            if (strength == PasswordEncoderUtils.PasswordStrength.WEAK) {
                throw new IllegalArgumentException("密码强度太弱");
            }
            
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("用户名已存在");
            }
            
            // 创建用户
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            
            // 加密密码
            String encodedPassword = passwordEncoderUtils.encode(password);
            user.setPassword(encodedPassword);
            
            // 保存用户
            userRepository.save(user);
            
            log.info("用户注册成功: {}", username);
            return true;
            
        } catch (Exception e) {
            log.error("用户注册失败: {}", username, e);
            return false;
        }
    }
}
```

### 2. 用户登录

```java
@Service
public class UserLoginService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    public boolean login(String username, String password) {
        try {
            // 查找用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                log.warn("用户不存在: {}", username);
                return false;
            }
            
            // 验证密码
            boolean isValid = passwordEncoderUtils.matchesAuto(password, user.getPassword());
            if (isValid) {
                log.info("用户登录成功: {}", username);
            } else {
                log.warn("密码验证失败: {}", username);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("用户登录异常: {}", username, e);
            return false;
        }
    }
}
```

### 3. 密码修改

```java
@Service
public class PasswordChangeService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        try {
            // 查找用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            
            // 验证旧密码
            if (!passwordEncoderUtils.matchesAuto(oldPassword, user.getPassword())) {
                throw new IllegalArgumentException("旧密码错误");
            }
            
            // 检查新密码强度
            PasswordEncoderUtils.PasswordStrength strength = passwordEncoderUtils.checkPasswordStrength(newPassword);
            if (strength == PasswordEncoderUtils.PasswordStrength.WEAK) {
                throw new IllegalArgumentException("新密码强度太弱");
            }
            
            // 加密新密码
            String encodedNewPassword = passwordEncoderUtils.encode(newPassword);
            user.setPassword(encodedNewPassword);
            
            // 更新用户
            userRepository.save(user);
            
            log.info("密码修改成功: {}", username);
            return true;
            
        } catch (Exception e) {
            log.error("密码修改失败: {}", username, e);
            return false;
        }
    }
}
```

### 4. 管理员重置密码

```java
@Service
public class AdminPasswordService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    public String resetUserPassword(String username) {
        try {
            // 查找用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            
            // 生成随机密码
            String newPassword = passwordEncoderUtils.generateRandomPassword(12);
            
            // 加密新密码
            String encodedPassword = passwordEncoderUtils.encode(newPassword);
            user.setPassword(encodedPassword);
            
            // 更新用户
            userRepository.save(user);
            
            log.info("管理员重置密码成功: {}", username);
            return newPassword; // 返回明文密码给管理员
            
        } catch (Exception e) {
            log.error("管理员重置密码失败: {}", username, e);
            throw e;
        }
    }
}
```

## Spring Security 集成

### 1. 配置密码编码器

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return passwordEncoderUtils.getBCryptEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
            )
            .passwordEncoder(passwordEncoder());
        
        return http.build();
    }
}
```

### 2. 自定义认证提供者

```java
@Service
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new BadCredentialsException("用户不存在");
        }
        
        if (!passwordEncoderUtils.matchesAuto(password, user.getPassword())) {
            throw new BadCredentialsException("密码错误");
        }
        
        return new UsernamePasswordAuthenticationToken(
            user, password, user.getAuthorities()
        );
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

## 最佳实践

### 1. 密码策略

```java
@Service
public class PasswordPolicyService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    /**
     * 验证密码策略
     */
    public PasswordPolicyResult validatePasswordPolicy(String password) {
        PasswordPolicyResult result = new PasswordPolicyResult();
        
        // 检查长度
        if (password.length() < 8) {
            result.addError("密码长度至少8位");
        }
        
        // 检查强度
        PasswordEncoderUtils.PasswordStrength strength = passwordEncoderUtils.checkPasswordStrength(password);
        if (strength == PasswordEncoderUtils.PasswordStrength.WEAK) {
            result.addError("密码强度太弱，请包含大小写字母、数字和特殊字符");
        }
        
        // 检查常见密码
        if (isCommonPassword(password)) {
            result.addError("不能使用常见密码");
        }
        
        return result;
    }
    
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
            "123456", "password", "admin", "qwerty", "abc123"
        };
        return Arrays.asList(commonPasswords).contains(password.toLowerCase());
    }
}
```

### 2. 密码历史

```java
@Service
public class PasswordHistoryService {
    
    @Autowired
    private PasswordEncoderUtils passwordEncoderUtils;
    
    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;
    
    /**
     * 检查密码历史
     */
    public boolean isPasswordReused(String username, String newPassword) {
        List<PasswordHistory> histories = passwordHistoryRepository.findByUsernameOrderByCreatedAtDesc(username);
        
        for (PasswordHistory history : histories) {
            if (passwordEncoderUtils.matchesAuto(newPassword, history.getPassword())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 保存密码历史
     */
    public void savePasswordHistory(String username, String encodedPassword) {
        PasswordHistory history = new PasswordHistory();
        history.setUsername(username);
        history.setPassword(encodedPassword);
        history.setCreatedAt(LocalDateTime.now());
        
        passwordHistoryRepository.save(history);
    }
}
```

## 配置说明

### 1. BCrypt配置

```yaml
synapse:
  security:
    password:
      bcrypt:
        strength: 12  # BCrypt强度（4-31）
```

### 2. SCrypt配置

```yaml
synapse:
  security:
    password:
      scrypt:
        cpu-cost: 16384
        memory-cost: 8
        parallelization: 1
        key-length: 32
```

## 性能考虑

### 1. 加密性能

| 算法 | 相对速度 | 安全性 | 推荐场景 |
|------|----------|--------|----------|
| BCrypt | 中等 | 高 | 通用推荐 |
| SCrypt | 慢 | 很高 | 高安全要求 |
| MD5 | 快 | 低 | 仅兼容 |
| SHA256 | 快 | 中等 | 需要加盐 |
| PBKDF2 | 慢 | 高 | 标准化要求 |

### 2. 性能优化建议

- **生产环境**: 使用BCrypt，强度设置为10-12
- **高安全环境**: 使用SCrypt
- **兼容性要求**: 支持多种算法自动检测
- **性能敏感**: 考虑使用SHA256+盐

## 故障排除

### 常见问题

1. **密码验证失败**
   - 检查加密算法是否匹配
   - 验证密码编码是否正确
   - 确认数据库存储格式

2. **性能问题**
   - 调整BCrypt强度参数
   - 考虑使用缓存
   - 优化数据库查询

3. **兼容性问题**
   - 使用自动检测功能
   - 支持多种算法
   - 提供迁移工具

## 版本历史

- **v1.0.0**: 初始版本，支持基础密码加密
- **v1.1.0**: 添加多种加密算法支持
- **v1.2.0**: 增加密码强度检查和随机生成
- **v1.3.0**: 完善Spring Security集成
- **v1.4.0**: 优化性能和错误处理
