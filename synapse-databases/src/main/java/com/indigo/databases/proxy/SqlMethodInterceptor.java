package com.indigo.databases.proxy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indigo.core.entity.dto.BaseDTO;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
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
            log.error("Critical error invoking method: {}", method.getName(), e);
            // 重新抛出原始异常，不要包装
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
        var repositoryInterface = method.getDeclaringClass();
        var genericInterfaces = repositoryInterface.getGenericInterfaces();

        for (var genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt &&
                    pt.getRawType().getTypeName().equals(BaseRepository.class.getName())) {

                var mapperClass = (Class<?>) pt.getActualTypeArguments()[1];
                try {
                    var mapper = staticApplicationContext.getBean(mapperClass);
                    return Optional.ofNullable(mapper);
                } catch (Exception e) {
                    log.error("Failed to get mapper instance for class: {}", mapperClass.getSimpleName(), e);
                }
            }
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


            // 对于其他default方法，我们需要直接执行其实现逻辑，而不是通过代理调用
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
                            if (args.length == 1 && args[0] instanceof PageDTO<?> queryDTO) {
                                yield EnhancedQueryBuilder.pageWithCondition((IService<?>) createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper), queryDTO);
                            } else if (args.length == 2 && args[0] instanceof PageDTO<?> queryDTO && args[1] instanceof Class<?> voClass) {
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

                        // 唯一性检查方法
                        case "checkKeyUniqueness" -> {
                            if (args.length >= 1) {
                                // 先调试原始参数
                                log.debug("checkKeyUniqueness - args.length: {}", args.length);
                                for (int i = 0; i < args.length; i++) {
                                    log.debug("args[{}]: type={}, value={}", i,
                                            args[i] != null ? args[i].getClass().getName() : "null",
                                            args[i]);
                                }

                                // 调试copyOfRange结果
                                Object[] rangeArgs = Arrays.copyOfRange(args, 1, args.length);
                                log.debug("rangeArgs (from 1 to {}): {}", args.length, Arrays.toString(rangeArgs));
                                
                                // 智能处理剩余参数：可能已经是String[]，也可能需要转换
                                java.util.List<String> keyFieldsList = new java.util.ArrayList<>();
                                for (Object arg : rangeArgs) {
                                    log.debug("Processing arg: type={}, value={}", 
                                             arg != null ? arg.getClass().getName() : "null", arg);
                                    
                                    if (arg instanceof String[]) {
                                        // 如果参数本身就是String[]，直接添加
                                        String[] stringArray = (String[]) arg;
                                        log.debug("Arg is String[], adding {} elements", stringArray.length);
                                        keyFieldsList.addAll(java.util.Arrays.asList(stringArray));
                                    } else {
                                        // 如果是单个对象，转换为String
                                        String stringValue = String.valueOf(arg);
                                        log.debug("Converting arg '{}' to String '{}'", arg, stringValue);
                                        keyFieldsList.add(stringValue);
                                    }
                                }
                                
                                String[] keyFields = keyFieldsList.toArray(new String[0]);

                                // 添加调试日志
                                log.debug("checkKeyUniqueness - keyFields: {}", Arrays.toString(keyFields));
                                // 额外调试每个keyField的类型
                                for (int i = 0; i < keyFields.length; i++) {
                                    log.debug("keyFields[{}]: type={}, value={}", i, keyFields[i].getClass().getName(), keyFields[i]);
                                }

                                // 直接实现 checkKeyUniqueness 的核心逻辑
                                Object firstArg = args[0];

                                // 特殊处理BaseDTO - 使用最保守的方法
                                if (firstArg instanceof BaseDTO<?> baseDTO) {
                                    log.debug("Processing BaseDTO checkKeyUniqueness - trying conservative approach");

                                    // 使用策略2：深层调试 + 智能恢复
                                    QueryWrapper<Object> wrapper = new QueryWrapper<>();

                                    // 智能处理keyFields中的每个元素
                                    for (String fieldNameParam : keyFields) {
                                        log.debug("Processing fieldNameParam: class={}, toString={}",
                                                fieldNameParam != null ? fieldNameParam.getClass().getName() : "null",
                                                fieldNameParam);

                                        // 解析出真正的字段名数组
                                        String[] fieldNames = parseFieldNames(fieldNameParam);
                                        log.debug("Parsed fieldNames: {}", Arrays.toString(fieldNames));

                                        // 处理每个字段名
                                        for (Object fieldItem : fieldNames) {
                                            log.debug("Processing field item: type={}, value={}", 
                                                     fieldItem != null ? fieldItem.getClass().getName() : "null",
                                                     fieldItem);
                                            
                                            // 最终的安全检查：确保fieldItem是String
                                            String actualFieldName;
                                            if (fieldItem instanceof String) {
                                                actualFieldName = (String) fieldItem;
                                            } else if (fieldItem instanceof String[]) {
                                                // 如果fieldItem是String[]，取第一个元素
                                                String[] array = (String[]) fieldItem;
                                                actualFieldName = array.length > 0 ? array[0] : "";
                                                log.warn("Field item was String[], extracting first element: '{}'", actualFieldName);
                                            } else {
                                                // 最后一个兜底：转换为String
                                                actualFieldName = String.valueOf(fieldItem);
                                                log.warn("Field item was unexpected type {}, converted to String: '{}'", 
                                                        fieldItem.getClass().getName(), actualFieldName);
                                            }
                                            
                                            log.debug("Final actualFieldName: '{}'", actualFieldName);

                                            try {
                                                Field field = baseDTO.getClass().getDeclaredField(actualFieldName);
                                                field.setAccessible(true);
                                                Object fieldValue = field.get(baseDTO);
                                                
                                                log.debug("Field '{}' value: {}", actualFieldName, fieldValue);

                                                if (fieldValue != null) {
                                                    wrapper.eq(actualFieldName, fieldValue);
                                                } else {
                                                    wrapper.isNull(actualFieldName);
                                                }
                                            } catch (Exception e) {
                                                log.error("Error accessing field '{}' in BaseDTO", actualFieldName, e);
                                                throw new RuntimeException("Failed to access field " + actualFieldName, e);
                                            }
                                        }
                                    }

                                    // 根据ID排除当前记录
                                    Object id = baseDTO.getId();
                                    if (id != null) {
                                        wrapper.ne("id", id);
                                    }

                                    // 执行查询
                                    var serviceObj = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                    IService<Object> service = (IService<Object>) serviceObj;
                                    yield service.count(wrapper) > 0;
                                }

                                if (firstArg == null || keyFields.length == 0) {
                                    yield false;
                                } else {
                                    // 创建ServiceImpl来执行查询操作
                                    var serviceObj = createServiceImplWithReflection(getEntityClass(proxy), getMapperClass(proxy), mapper);
                                    IService<?> service = (IService<?>) serviceObj;

                                    // 构建查询条件 - 使用强制转换避免类型问题
                                    @SuppressWarnings("unchecked")
                                    QueryWrapper<Object> queryWrapper = (QueryWrapper<Object>) buildUniquenessWrapper(getEntityClass(proxy), firstArg, keyFields);

                                    // 根据ID排除当前记录（如果是更新场景）
                                    Object id = getIdFromObject(firstArg);
                                    if (id != null) {
                                        queryWrapper.ne("id", id);
                                    }

                                    // 执行查询，如果存在记录则说明有重复 - 强制转换为IService<Object>
                                    IService<Object> objectService = (IService<Object>) service;
                                    yield objectService.count(queryWrapper) > 0;
                                }
                            }
                            throw new UnsupportedOperationException("Invalid arguments for checkKeyUniqueness");
                        }

                        default ->
                                throw new UnsupportedOperationException("Default method not implemented: " + methodName);
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

    /**
     * 构建唯一性检查的查询条件
     */
    private <T> QueryWrapper<T> buildUniquenessWrapper(Class<T> entityClass, Object object, String[] keyFields) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        // 添加调试日志
        log.debug("Building uniqueness wrapper for entity: {}, keyFields: {}", entityClass.getName(), Arrays.toString(keyFields));
        for (String field : keyFields) {
            if (field == null || field.trim().isEmpty()) {
                log.warn("Skipping null or empty field name");
                continue;
            }

            try {
                log.debug("Processing field: {}", field);
                Object fieldValue = extractFieldValue(object, field);

                if (fieldValue != null) {
                    wrapper.eq(field, fieldValue);
                } else {
                    wrapper.isNull(field);
                }
            } catch (Exception ex) {
                log.error("Critical error accessing field '{}' for object {}, failing uniqueness check", field, object.getClass().getName(), ex);
                throw new RuntimeException("Failed to access field " + field + " for uniqueness check", ex);
            }
        }

        return wrapper;
    }

    /**
     * 提取字段值（支持getter方法和直接字段访问）
     */
    private Object extractFieldValue(Object object, String fieldName) throws Exception {
        // 添加调试日志，检查fieldName的类型
        log.debug("extractFieldValue - object class: {}, fieldName type: {}, fieldName: {}",
                object.getClass().getName(), fieldName.getClass().getName(), fieldName);

        String getterMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // 首先尝试使用getter方法
        try {
            java.lang.reflect.Method getter = object.getClass().getMethod(getterMethodName);
            return getter.invoke(object);
        } catch (NoSuchMethodException getterEx) {
            // 如果getter方法不存在，尝试直接访问字段
            log.debug("Getter method {} not found, trying direct field access", getterMethodName);

            Field javaField = object.getClass().getDeclaredField(fieldName);
            javaField.setAccessible(true);
            return javaField.get(object);
        }
    }

    /**
     * 从对象中提取ID值
     */
    private Object getIdFromObject(Object object) {
        try {
            return extractFieldValue(object, "id");
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            // 如果ID字段和方法都不存在，这是正常情况（新增场景）
            log.debug("No id field/getter found for object {}, treating as new entity", object.getClass().getName());
            return null;
        } catch (Exception ex) {
            // 其他异常表明程序出错，应该中断执行
            log.error("Failed to extract ID from object {}", object.getClass().getName(), ex);
            throw new RuntimeException("Failed to extract ID from object", ex);
        }
    }

    /**
     * 智能解析字段名参数，支持多种输入格式
     */
    private String[] parseFieldNames(Object fieldNameParam) {
        log.debug("Entering parseFieldNames with: class={}, toString={}", 
                 fieldNameParam != null ? fieldNameParam.getClass().getName() : "null",
                 fieldNameParam);
                 
        if (fieldNameParam instanceof String[]) {
            // 如果本身就是String[]，直接使用
            String[] fieldArray = (String[]) fieldNameParam;
            log.debug("fieldNameParam is String[], returning: {}", Arrays.toString(fieldArray));
            
            // 但我们需要确保返回的数组中每个元素都是String，而不是String[]
            for (int i = 0; i < fieldArray.length; i++) {
                Object item = fieldArray[i];
                if (!(item instanceof String)) {
                    log.error("String[] contains non-String element at index {}: class={}, value={}", 
                             i, item != null ? item.getClass().getName() : "null", item);
                    throw new RuntimeException("String[] contains non-String element at index " + i);
                }
            }
            
            return fieldArray;
        } else if (fieldNameParam instanceof String) {
            // 如果是String，包装成数组
            String fieldName = (String) fieldNameParam;
            String[] result = {fieldName};
            log.debug("fieldNameParam is String '{}', wrapping to Array", fieldName);
            return result;
        } else if (fieldNameParam instanceof java.util.List) {
            // 如果是List，转换为String数组
            @SuppressWarnings("rawtypes")
            java.util.List list = (java.util.List) fieldNameParam;
            String[] result = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof String[]) {
                    // 如果List中包含String[]，取第一个元素
                    String[] innerArray = (String[]) item;
                    result[i] = innerArray.length > 0 ? innerArray[0] : "";
                    log.debug("List[{}] contains String[], extracting first element: {}", i, result[i]);
                } else {
                    // 否则转换为String
                    result[i] = String.valueOf(item);
                    log.debug("List[{}] converted to String: {}", i, result[i]);
                }
            }
            return result;
        } else if (fieldNameParam.getClass().isArray()) {
            // 如果是其他类型的数组，尝试转换为String数组
            Object[] array = (Object[]) fieldNameParam;
            String[] result = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                Object item = array[i];
                if (item instanceof String[]) {
                    // 嵌套数组情况
                    String[] innerArray = (String[]) item;
                    result[i] = innerArray.length > 0 ? innerArray[0] : "";
                    log.debug("Array[{}] contains String[], extracting first element: {}", i, result[i]);
                } else {
                    result[i] = String.valueOf(item);
                    log.debug("Array[{}] converted to String: {}", i, result[i]);
                }
            }
            return result;
        } else {
            // 未知类型，尝试转换为String
            log.warn("Unknown fieldNameParam type: {}, converting to String", fieldNameParam.getClass().getName());
            return new String[]{String.valueOf(fieldNameParam)};
        }
    }

} 