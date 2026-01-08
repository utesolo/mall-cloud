# 农业供销平台 - AI Agent 开发指南

本文档为AI开发助手提供项目技术栈、编码规范和最佳实践指导。

## 📚 技术栈

### 核心框架
- **Java**: 25
- **Apache Maven**: 3.8.4
- **Spring Boot**: 3.5.8
- **Spring Cloud**: 2025.0.0
- **Alibaba Nacos**: 2025.0.0.0 (服务发现与配置中心)
- **OpenFeign**: 4.3.0 (微服务调用)

### 数据库与缓存
- **MySQL**: 8.0.39 (主数据库)
- **Redis**: 5.0.14.1 (缓存与限流)
- **Spring Data Redis**: 3.5.8
- **MyBatis Plus**: 3.5.9 (ORM框架)
- **Elasticsearch**: 7.17.5 (搜索引擎)
- **Spring Data Elasticsearch**

### 工具库
- **Slf4j**: 2.0.17 (日志门面)
- **Lombok** (代码简化)
- **Fastjson2** (JSON处理)
- **AssertJ Core**: 3.26.3 (测试断言)
- **JWT**: 0.12.5 (认证令牌)
- **OpenAPI/Swagger** (API文档)

### 链路追踪
- **Zipkin Reporter Brave**: 2.17.0
- **Micrometer Tracing**: 1.6.0-M1

## 📝 编码规范

### 1. 类与方法规范

#### 1.1 类结构
```java
/**
 * 类说明：描述类的功能和职责
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j                    // 日志注解
@Service                  // 或 @Controller, @Component
@RequiredArgsConstructor  // Lombok构造器注入
public class ExampleService {
    
    private final DependencyA dependencyA;
    private final DependencyB dependencyB;
    
    // 静态常量定义
    private static final String CONSTANT_VALUE = "value";
    
    // 公共方法
    // 私有方法
}
```

#### 1.2 方法注释（强制要求）
每个方法必须包含完整的JavaDoc注释：
```java
/**
 * 方法功能描述
 * 详细说明方法的业务逻辑和处理流程
 * 
 * @param param1 参数1说明
 * @param param2 参数2说明
 * @return 返回值说明
 * @throws BusinessException 抛出的业务异常及原因
 */
public Result<Data> methodName(Type param1, Type param2) {
    // 实现逻辑
}
```

#### 1.3 参数封装规则
**当方法参数超过3个时，必须封装为DTO对象**
```java
// ❌ 错误示例
public void createOrder(Long userId, Long productId, Integer quantity, 
                        String address, String phone, String remark) {
}

// ✅ 正确示例
public void createOrder(CreateOrderDTO orderDTO) {
}
```

### 2. 日志规范

#### 2.1 日志级别使用
- **DEBUG**: 详细的调试信息
- **INFO**: 重要业务流程节点
- **WARN**: 警告信息（业务异常）
- **ERROR**: 错误信息（系统异常）

```java
// 业务流程日志
log.info("创建订单成功, orderNo: {}, userId: {}, productId: {}", 
        orderNo, userId, productId);

// 调试信息
log.debug("查询商品信息, productId: {}", productId);

// 警告信息
log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());

// 错误信息
log.error("系统异常: ", e);
```

#### 2.2 日志实现方式
- 使用 `@Slf4j` 注解自动注入日志对象
- 日志通过AOP切面统一处理，精确到方法级别
- 避免在代码中硬编码日志对象

### 3. 异常处理

#### 3.1 统一异常处理
项目使用 `GlobalExceptionHandler` 统一处理所有异常
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
```

#### 3.2 业务异常抛出
```java
// 使用自定义业务异常
if (product == null) {
    throw new BusinessException("商品不存在或已下架");
}

if (stock < quantity) {
    throw new BusinessException("库存不足");
}
```

### 4. 返回值规范

#### 4.1 Controller统一返回格式
所有Controller接口必须返回 `Result<T>` 类型
```java
@RestController
@RequestMapping("/example")
public class ExampleController {
    
    @GetMapping("/data")
    public Result<DataVO> getData() {
        DataVO data = service.getData();
        return Result.success(data, "查询成功");
    }
    
    @PostMapping("/create")
    public Result<Void> create(@RequestBody CreateDTO dto) {
        service.create(dto);
        return Result.success(null, "创建成功");
    }
}
```

#### 4.2 Result响应结构
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 5. 策略模式使用

**业务逻辑中避免大量if-else，使用策略模式**
```java
// ❌ 避免这样写
if (type.equals("A")) {
    // 处理A
} else if (type.equals("B")) {
    // 处理B
} else if (type.equals("C")) {
    // 处理C
}

// ✅ 推荐使用策略模式
@Component
public class StrategyFactory {
    private final Map<String, Strategy> strategyMap;
    
    public Strategy getStrategy(String type) {
        return strategyMap.get(type);
    }
}
```

### 6. 数据库操作规范

#### 6.1 MyBatis-Plus使用
```java
// 使用LambdaQueryWrapper避免硬编码字段名
LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(User::getOpenid, openid);
queryWrapper.eq(User::getStatus, "NORMAL");
User user = userMapper.selectOne(queryWrapper);
```

#### 6.2 事务管理
```java
@Transactional(rollbackFor = Exception.class)
public void businessMethod() {
    // 业务逻辑
    // 发生任何异常都会回滚
}
```

### 7. 限流注解使用

对需要限流的接口使用 `@RateLimit` 注解
```java
@PostMapping("/login")
@RateLimit(prefix = "login", window = 60, maxRequests = 50, 
          message = "登录请求过于频繁，请稍后再试")
public Result<LoginVO> login(@RequestBody LoginDTO dto) {
    // 登录逻辑
}
```

### 8. API文档注解

使用OpenAPI注解提供完整的API文档
```java
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "用户登录、注册、退出等认证相关接口")
public class AuthController {
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过账号密码进行登录")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        // 实现
    }
}
```

### 9. DTO/VO/Entity命名规范

- **DTO**: Data Transfer Object，用于接收请求参数
  - 例如: `CreateOrderDTO`, `WeChatLoginDTO`
- **VO**: View Object，用于返回给前端的数据
  - 例如: `OrderVO`, `LoginVO`
- **Entity**: 数据库实体对象
  - 例如: `Order`, `User`, `Product`

### 10. TODO注释规范

使用TODO标记待实现或待优化的功能
```java
// TODO 功能描述：说明需要实现的具体功能或优化方向
// TODO 折扣功能，部分商品添加特价等活动折扣
// TODO 数据传输加密
// TODO 通过微信小程序来实现支付，支付在小程序完成，返回给后端一个结果
```

## 🏗️ 架构设计原则

### 1. 微服务划分
- **mall-gateway**: API网关，路由转发、限流、认证
- **mall-auth**: 认证服务，用户登录、JWT管理
- **mall-product**: 商品服务，商品管理、库存管理
- **mall-trade**: 交易服务，订单、购物车、匹配
- **mall-search**: 搜索服务，ES商品搜索
- **mall-job**: 定时任务服务，数据同步、缓存刷新
- **mall-common**: 公共模块，工具类、异常处理

### 2. 服务调用
- 服务间通过OpenFeign调用
- 需要配置降级策略（Fallback）
- 超时时间合理设置（connect-timeout, read-timeout）

### 3. 数据一致性
- 使用 `@Transactional` 保证本地事务
- 分布式事务使用TCC模式或最终一致性
- 事务提交后使用 `TransactionSynchronizationManager` 注册异步任务

### 4. 异步处理
使用 `CompletableFuture` 和线程池进行异步处理
```java
CompletableFuture<ProductDTO> productFuture = CompletableFuture.supplyAsync(
    () -> getProductInfo(productId), orderExecutor);

CompletableFuture<Void> deductFuture = CompletableFuture.runAsync(
    () -> deductStock(productId, quantity), orderExecutor);

CompletableFuture.allOf(productFuture, deductFuture).join();
```

### 5. 配置管理
- 配置统一存储在Nacos配置中心
- 本地配置文件只保留引导配置
- 通用配置抽取为共享配置（如mysql-common.yml, redis-common.yml）

## 🔐 安全规范

### 1. 认证授权
- 使用JWT双Token机制（Access Token + Refresh Token）
- Access Token有效期15分钟，Refresh Token有效期7天
- 每个Token包含唯一的JTI标识符
- Token支持撤销和黑名单机制

### 2. 限流保护
- 网关层：IP限流（1分钟100次）
- 业务层：用户限流（1分钟50次）
- 使用Redis + Lua脚本实现滑动窗口算法

### 3. 数据校验
- 使用 `@Valid` 和 `@Validated` 进行参数校验
- DTO中添加校验注解（@NotNull, @NotBlank, @Min, @Max等）

## 🧪 测试规范

### 1. 单元测试
- 每个Service类必须有对应的测试类
- 测试类命名：`XxxServiceTest`
- 使用AssertJ进行断言
```java
@SpringBootTest
class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testCreateOrder() {
        // given
        CreateOrderDTO dto = new CreateOrderDTO();
        // ...
        
        // when
        OrderVO result = orderService.createOrder(dto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isNotBlank();
    }
}
```

### 2. 测试覆盖率
- 核心业务逻辑测试覆盖率要求 > 80%
- 关键路径必须有测试用例

## 📦 依赖注入规范

### 1. 推荐使用构造器注入
```java
@Service
@RequiredArgsConstructor  // Lombok生成构造器
public class ExampleService {
    
    private final DependencyA dependencyA;  // final确保不可变
    private final DependencyB dependencyB;
}
```

### 2. 避免使用字段注入
```java
// ❌ 不推荐
@Autowired
private DependencyA dependencyA;

// ✅ 推荐
private final DependencyA dependencyA;

public ExampleService(DependencyA dependencyA) {
    this.dependencyA = dependencyA;
}
```

## 🔍 代码审查检查点

开发完成后请检查以下内容：

- [ ] 所有方法都有完整的JavaDoc注释
- [ ] 超过3个参数的方法已封装为DTO
- [ ] Controller返回统一的Result<T>格式
- [ ] 使用了@Slf4j注解，日志级别正确
- [ ] 异常处理符合规范，使用BusinessException
- [ ] 避免了大量if-else，考虑使用策略模式
- [ ] 数据库操作使用LambdaQueryWrapper
- [ ] 需要事务的方法添加了@Transactional
- [ ] 需要限流的接口添加了@RateLimit
- [ ] API文档注解完整（@Tag, @Operation）
- [ ] 重要业务逻辑有单元测试
- [ ] 使用构造器注入而非字段注入
- [ ] TODO注释清晰明确

## 📚 参考资源

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus官方文档](https://baomidou.com/)
- [Nacos官方文档](https://nacos.io/)
- [OpenAPI规范](https://swagger.io/specification/)

---

**最后更新**: 2026-01-08  
**维护团队**: lynz
**版本**: 1.0.0