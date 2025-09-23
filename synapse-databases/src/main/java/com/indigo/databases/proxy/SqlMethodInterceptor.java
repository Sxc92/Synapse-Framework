package com.indigo.databases.proxy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.dto.page.*;
import com.indigo.core.entity.vo.BaseVO;
import com.indigo.databases.utils.EnhancedQueryBuilder;
import com.indigo.databases.repository.BaseRepository;
import com.indigo.databases.utils.QueryConditionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * SQL方法拦截器
 * 用于动态代理处理MyBatis-Plus注解SQL方法
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@Component
public class SqlMethodInterceptor implements InvocationHandler {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 静态引用，用于在动态代理中访问ApplicationContext
    private static ApplicationContext staticApplicationContext;
    
    @PostConstruct
    public void init() {
        // 立即初始化ApplicationContext，确保Bean注册时可用
        staticApplicationContext = this.applicationContext;
        log.info("SqlMethodInterceptor ApplicationContext 初始化完成");
        
        // 检查是否存在Seata相关类，仅记录日志
        try {
            Class.forName("io.seata.spring.annotation.GlobalTransactional");
            log.info("检测到Seata环境，SqlMethodInterceptor已准备就绪");
        } catch (ClassNotFoundException e) {
            log.debug("未检测到Seata环境，SqlMethodInterceptor正常初始化");
        }
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // 检查是否是Object的方法
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            // 检查是否是BaseRepository的方法
            if (isBaseRepositoryMethod(method)) {
                return handleBaseRepositoryMethod(proxy, method, args);
            }
            // 检查是否是Mapper的方法
            if (isMapperMethod(method)) {
                return callMapperMethod(method, args);
            }
            // 如果没有SQL注解，尝试调用默认实现
            if (method.isDefault()) {
                return method.invoke(proxy, args);
            }
            // 如果既没有SQL注解也不是默认方法，抛出异常
            throw new UnsupportedOperationException(
                "Method " + method.getName() + " has no default implementation"
            );
        } catch (Exception e) {
            log.error("Error invoking method: {}", method.getName(), e);
            throw e;
        }
    }
    
    /**
     * 调用mapper方法
     */
    private Object callMapperMethod(Method method, Object[] args) throws Exception {
        var mapper = getMapperInstance(method);
        
        // 尝试在mapper上调用相同的方法
        try {
            var mapperMethod = mapper.getClass().getMethod(method.getName(), method.getParameterTypes());
            return mapperMethod.invoke(mapper, args);
        } catch (NoSuchMethodException e) {
            // 如果mapper上没有对应方法，抛出异常
            throw new UnsupportedOperationException(
                "Method " + method.getName() + " not found on mapper"
            );
        }
    }
    
    /**
     * 检查是否是BaseRepository的方法
     */
    private boolean isBaseRepositoryMethod(Method method) {
        var declaringClass = method.getDeclaringClass();
        // 检查是否是BaseRepository或其父接口的方法
        return BaseRepository.class.isAssignableFrom(declaringClass) || 
               method.getDeclaringClass().getName().contains("IService") ||
               IService.class.isAssignableFrom(declaringClass);
    }
    
    /**
     * 检查是否是Mapper的方法
     */
    private boolean isMapperMethod(Method method) {
        var declaringClass = method.getDeclaringClass();
        // 检查是否是BaseMapper或其子接口的方法
        return com.baomidou.mybatisplus.core.mapper.BaseMapper.class.isAssignableFrom(declaringClass) ||
               declaringClass.getName().contains("Mapper");
    }
    
    /**
     * 使用Spring BeanFactory创建ServiceImpl实例（最终方案）
     */
    private Object createServiceImplWithReflection(Class<?> entityClass, Class<?> mapperClass, Object mapper) {
        try {
            // 使用Spring的BeanFactory创建
            var beanFactory = staticApplicationContext.getAutowireCapableBeanFactory();
            
            // 创建ServiceImpl的匿名实现类，使用正确的泛型参数
            @SuppressWarnings("rawtypes")
            var serviceImpl = new ServiceImpl() {
                @Override
                public BaseMapper getBaseMapper() {
                    return (BaseMapper) mapper;
                }
            };
            
            // 自动装配依赖
            beanFactory.autowireBean(serviceImpl);
            beanFactory.initializeBean(serviceImpl, "serviceImpl");
            
            log.debug("Created ServiceImpl instance for entity: {}, mapper: {} using Spring BeanFactory", 
                     entityClass.getSimpleName(), mapperClass.getSimpleName());
            return serviceImpl;
        } catch (Exception e) {
            log.error("Failed to create ServiceImpl for entity: {}, mapper: {}", 
                     entityClass.getSimpleName(), mapperClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create ServiceImpl", e);
        }
    }
    
    /**
     * 在ServiceImpl上查找与Repository方法兼容的方法（同名、参数可赋值）
     */
    private Method findCompatibleMethod(Class<?> serviceImplClass, Method repoMethod, Object[] args) {
        return findCompatibleMethod(serviceImplClass, repoMethod.getName(), args);
    }
    
    /**
     * 在ServiceImpl上查找与指定方法名兼容的方法（同名、参数可赋值）
     */
    private Method findCompatibleMethod(Class<?> serviceImplClass, String methodName, Object[] args) {
        var methods = serviceImplClass.getMethods();
        
        outer:
        for (var m : methods) {
            if (!m.getName().equals(methodName)) continue;
            
            var paramTypes = m.getParameterTypes();
            if (paramTypes.length != (args == null ? 0 : args.length)) continue;
            
            for (int i = 0; i < paramTypes.length; i++) {
                if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                    continue outer;
                }
            }
            return m;
        }
        return null;
    }
    
    /**
     * 获取Mapper实例，使用Optional优化空值处理
     */
    private Optional<Object> getMapperInstanceOptional(Method method) {
        try {
            // 获取Repository接口的泛型参数
            var repositoryInterface = method.getDeclaringClass();
            var genericInterfaces = repositoryInterface.getGenericInterfaces();
            for (var genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType pt) {
                    if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                        var mapperClass = (Class<?>) pt.getActualTypeArguments()[1];
                        
                        return Optional.ofNullable(staticApplicationContext)
                            .map(context -> {
                                try {
                                    return context.getBean(mapperClass);
                                } catch (Exception e) {
                                    log.error("Failed to get mapper instance for class: {}", mapperClass.getSimpleName(), e);
                                    return null;
                                }
                            });
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error resolving mapper for method: {}", method.getName(), e);
        }
        return Optional.empty();
    }
    
    /**
     * 获取Mapper实例（保持向后兼容）
     */
    private Object getMapperInstance(Method method) throws Exception {
        return getMapperInstanceOptional(method)
            .orElseThrow(() -> new RuntimeException("Cannot resolve Mapper for method: " + method.getName()));
    }
    
    /**
     * 处理BaseRepository的方法
     */
    private Object handleBaseRepositoryMethod(Object proxy, Method method, Object[] args) throws Exception {
        log.info("Handling BaseRepository method: {}", method.getName());
        log.info("Method isDefault: {}, declaringClass: {}", method.isDefault(), method.getDeclaringClass().getName());
        
        // 特殊处理getBaseMapper方法
        if ("getBaseMapper".equals(method.getName())) {
            return handleGetBaseMapper(proxy);
        }
        
        // 处理default方法
        if (method.isDefault()) {
            log.info("Calling default method: {} with args: {}", method.getName(), args);
            // 对于default方法，我们需要直接执行其实现逻辑，而不是通过代理调用
            return executeDefaultMethod(proxy, method, args);
        }
        
        // 对于非default方法（如IService的方法），直接调用IService实现
        log.info("Calling IService method: {} with args: {}", method.getName(), args);
        return callIServiceMethod(proxy, method.getName(), args);
    }
    
    /**
     * 执行default方法的实现逻辑
     */
    private Object executeDefaultMethod(Object proxy, Method method, Object[] args) throws Exception {
        var methodName = method.getName();
        
        // 获取Repository接口的泛型参数
        var repositoryInterface = proxy.getClass().getInterfaces()[0];
        var genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                    var mapperClass = (Class<?>) pt.getActualTypeArguments()[1];
                    
                    // 获取Mapper实例
                    if (staticApplicationContext == null) {
                        throw new RuntimeException("ApplicationContext not initialized");
                    }
                    var mapper = staticApplicationContext.getBean(mapperClass);
                    
                    // 使用现代switch表达式处理不同的方法
                    return switch (methodName) {
                        // IService 方法处理
                        case "save", "getById", "updateById", "removeById", "list", "page", "count", "getOne" -> 
                            callIServiceMethod(proxy, methodName, args);
                        
                        // BaseRepository default 方法处理
                        case "pageWithCondition" -> {
                            if (args.length == 1 && args[0] instanceof PageDTO queryDTO) {
                                yield EnhancedQueryBuilder.pageWithCondition((IService<?>) createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper), queryDTO);
                            } else if (args.length == 2 && args[0] instanceof PageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                yield EnhancedQueryBuilder.pageWithCondition((IService<?>) createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper), queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithCondition");
                        }
                        
                        case "listWithCondition", "listWithDTO" -> {
                            if (args.length == 1) {
                                var wrapper = QueryConditionBuilder.buildQueryWrapper(args[0]);
                                yield callIServiceMethod(proxy, "list", new Object[]{wrapper});
                            } else if (args.length == 2 && args[1] instanceof Class<?> voClass) {
                                yield EnhancedQueryBuilder.listWithCondition((IService<?>) createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper), (QueryDTO) args[0], (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for " + methodName);
                        }
                        
                        case "getOneWithCondition", "getOneWithDTO" -> {
                            if (args.length == 1) {
                                var wrapper = QueryConditionBuilder.buildQueryWrapper(args[0]);
                                yield callIServiceMethod(proxy, "getOne", new Object[]{wrapper});
                            } else if (args.length == 2 && args[1] instanceof Class<?> voClass) {
                                yield EnhancedQueryBuilder.getOneWithCondition((IService<?>) createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper), (QueryDTO) args[0], (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for " + methodName);
                        }
                        
                        case "countWithCondition", "countWithDTO" -> {
                            if (args.length == 1) {
                                var wrapper = QueryConditionBuilder.buildQueryWrapper(args[0]);
                                yield callIServiceMethod(proxy, "count", new Object[]{wrapper});
                            }
                            throw new UnsupportedOperationException("Invalid arguments for " + methodName);
                        }
                        
                        // 增强查询方法
                        case "pageWithAggregation" -> {
                            if (args.length == 2 && args[0] instanceof AggregationPageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithAggregation((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithAggregation - requires AggregationPageDTO and VO class");
                        }
                        
                        case "pageWithGroupBy" -> {
                            if (args.length == 2 && args[0] instanceof AggregationPageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithGroupBy((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithGroupBy - requires AggregationPageDTO and VO class");
                        }
                        
                        case "pageWithPerformance" -> {
                            if (args.length == 2 && args[0] instanceof PerformancePageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithPerformance((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithPerformance - requires PerformancePageDTO and VO class");
                        }
                        
                        case "pageWithSelectFields" -> {
                            if (args.length == 2 && args[0] instanceof PerformancePageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithSelectFields((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithSelectFields - requires PerformancePageDTO and VO class");
                        }
                        
                        case "pageWithVoMapping" -> {
                            if (args.length == 2 && args[0] instanceof PageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithCondition((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithVoMapping");
                        }
                        
                        case "pageWithComplexQuery" -> {
                            if (args.length == 2 && args[0] instanceof ComplexPageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithComplexQuery((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithComplexQuery - requires ComplexPageDTO and VO class");
                        }
                        
                        case "pageWithEnhanced" -> {
                            if (args.length == 2 && args[0] instanceof EnhancedPageDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithEnhanced((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithEnhanced - requires EnhancedPageDTO and VO class");
                        }
                        
                        // 便捷查询方法
                        case "quickPage" -> {
                            if (args.length == 2 && args[0] instanceof PageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.quickPage((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickPage");
                        }
                        
                        case "quickList" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.quickList((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickList");
                        }
                        
                        case "quickGetOne" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.quickGetOne((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickGetOne");
                        }
                        
                        // 异步查询方法
                        case "pageWithConditionAsync" -> {
                            if (args.length == 2 && args[0] instanceof PageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithConditionAsync((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithConditionAsync");
                        }
                        
                        case "listWithConditionAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.listWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for listWithConditionAsync");
                        }
                        
                        case "getOneWithConditionAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.getOneWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for getOneWithConditionAsync");
                        }
                        
                        case "pageWithPerformanceAsync" -> {
                            if (args.length == 2 && args[0] instanceof PerformancePageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithPerformanceAsync((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithPerformanceAsync");
                        }
                        
                        case "pageWithAggregationAsync" -> {
                            if (args.length == 2 && args[0] instanceof AggregationPageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithAggregationAsync((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithAggregationAsync");
                        }
                        
                        case "pageWithEnhancedAsync" -> {
                            if (args.length == 2 && args[0] instanceof EnhancedPageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithEnhancedAsync((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for pageWithEnhancedAsync");
                        }
                        
                        case "countWithConditionAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.countWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for countWithConditionAsync");
                        }
                        
                        case "existsWithConditionAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.existsWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for existsWithConditionAsync");
                        }
                        
                        case "quickPageAsync" -> {
                            if (args.length == 2 && args[0] instanceof PageDTO pageDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.pageWithConditionAsync((IService<?>) service, pageDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickPageAsync");
                        }
                        
                        case "quickListAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.listWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickListAsync");
                        }
                        
                        case "quickGetOneAsync" -> {
                            if (args.length == 2 && args[0] instanceof QueryDTO queryDTO && args[1] instanceof Class<?> voClass) {
                                var service = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                yield EnhancedQueryBuilder.getOneWithConditionAsync((IService<?>) service, queryDTO, (Class<? extends BaseVO>) voClass);
                            }
                            throw new UnsupportedOperationException("Invalid arguments for quickGetOneAsync");
                        }
                        
                        default -> throw new UnsupportedOperationException("Default method not implemented: " + methodName);
                    };
                }
            }
        }
        
        throw new UnsupportedOperationException("Default method not implemented: " + methodName);
    }
    
    /**
     * 调用IService方法
     */
    private Object callIServiceMethod(Object proxy, String methodName, Object[] args) throws Exception {
        // 获取Repository接口的泛型参数
        var repositoryInterface = proxy.getClass().getInterfaces()[0];
        var genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                    var entityClass = (Class<?>) pt.getActualTypeArguments()[0];
                    var mapperClass = (Class<?>) pt.getActualTypeArguments()[1];
                    
                    // 获取Mapper实例
                    if (staticApplicationContext == null) {
                        throw new RuntimeException("ApplicationContext not initialized");
                    }
                    var mapper = staticApplicationContext.getBean(mapperClass);
                    
                    // 创建ServiceImpl实例
                    var serviceImpl = createServiceImplWithReflection(entityClass, mapperClass, mapper);
                    
                    // 调用IService方法
                    var serviceMethod = findCompatibleMethod(serviceImpl.getClass(), methodName, args);
                    if (serviceMethod != null) {
                        return serviceMethod.invoke(serviceImpl, args);
                    }
                }
            }
        }
        
        throw new UnsupportedOperationException("IService method not found: " + methodName);
    }

    /**
     * 处理getBaseMapper方法
     */
    private Object handleGetBaseMapper(Object proxy) throws Exception {
        // 获取Repository接口的泛型参数
        var repositoryInterface = proxy.getClass().getInterfaces()[0];
        var genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                    var mapperClass = (Class<?>) pt.getActualTypeArguments()[1];
                    
                    if (staticApplicationContext == null) {
                        throw new RuntimeException("ApplicationContext not initialized");
                    }
                    return staticApplicationContext.getBean(mapperClass);
                }
            }
        }
        throw new RuntimeException("Cannot resolve Mapper for getBaseMapper method");
    }
    
    /**
     * 获取Repository接口的泛型参数中的实体类
     */
    private Class<?> getEntityClass(Object proxy) {
        var repositoryInterface = proxy.getClass().getInterfaces()[0];
        var genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                    return (Class<?>) pt.getActualTypeArguments()[0];
                }
            }
        }
        throw new RuntimeException("Cannot resolve entity class for proxy: " + proxy.getClass().getName());
    }

    /**
     * 获取Repository接口的泛型参数中的Mapper类
     */
    private Class<?> getMapperClass(Object proxy) {
        var repositoryInterface = proxy.getClass().getInterfaces()[0];
        var genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {
                    return (Class<?>) pt.getActualTypeArguments()[1];
                }
            }
        }
        throw new RuntimeException("Cannot resolve mapper class for proxy: " + proxy.getClass().getName());
    }

} 