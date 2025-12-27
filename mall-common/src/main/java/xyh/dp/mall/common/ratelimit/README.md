# 限流功能说明

## 功能概述

基于Redis实现的高性能滑动窗口限流功能，用于防止脚本快速重复请求，保护系统资源。

## 核心组件

### 1. RateLimit注解
标记需要限流的接口方法。

**参数说明:**
- `prefix`: 限流key前缀（默认："rate_limit"）
- `window`: 时间窗口，单位秒（默认：60秒）
- `maxRequests`: 时间窗口内最大请求次数（默认：50次）
- `message`: 限流提示信息（默认："请求过于频繁，请稍后再试"）

### 2. RateLimiter组件
基于Redis的滑动窗口限流器，使用Lua脚本保证原子性。

**核心方法:**
- `tryAcquire(key, window, maxRequests)`: 尝试获取访问许可
- `getCurrentCount(key, window)`: 获取当前窗口内的请求次数
- `clear(key)`: 清空限流记录

### 3. RateLimitAspect切面
拦截带有@RateLimit注解的方法，自动执行限流检查。

**工作流程:**
1. 从UserContext获取当前用户ID
2. 构建限流key: `prefix:userId`
3. 调用RateLimiter检查是否允许访问
4. 如果超限，抛出BusinessException

## 使用示例

### 基础使用
```java
@RestController
@RequestMapping("/api")
public class DemoController {
    
    /**
     * 限流示例：1分钟内最多50次请求
     */
    @PostMapping("/action")
    @RequireLogin
    @RateLimit(prefix = "api_action", window = 60, maxRequests = 50)
    public Result<Void> action() {
        // 业务逻辑
        return Result.success();
    }
}
```

### 自定义限流配置
```java
/**
 * 登录接口：1分钟内最多10次请求
 */
@PostMapping("/login")
@RateLimit(
    prefix = "login", 
    window = 60, 
    maxRequests = 10, 
    message = "登录请求过于频繁，请1分钟后再试"
)
public Result<LoginVO> login(@RequestBody LoginDTO dto) {
    // 登录逻辑
}

/**
 * 创建订单：1分钟内最多30次请求
 */
@PostMapping("/order/create")
@RequireLogin
@RateLimit(
    prefix = "order_create", 
    window = 60, 
    maxRequests = 30, 
    message = "创建订单过于频繁，请稍后再试"
)
public Result<OrderVO> createOrder(@RequestBody CreateOrderDTO dto) {
    // 创建订单逻辑
}
```

## 技术实现

### 滑动窗口算法
使用Redis的SortedSet（ZSET）实现滑动窗口：
- 成员：请求时间戳
- 分数：请求时间戳
- 时间复杂度：O(log N)

**Lua脚本逻辑:**
```lua
1. 移除窗口外的旧记录（ZREMRANGEBYSCORE）
2. 统计窗口内的请求次数（ZCARD）
3. 如果未超限，添加新记录（ZADD）
4. 设置key过期时间（EXPIRE）
```

### 降级策略
当Redis不可用时，自动降级为允许访问，确保系统可用性。

## 已应用的接口

以下接口已配置限流（1分钟50次）：

### 认证服务 (mall-auth)
- `POST /auth/wechat/login` - 微信登录

### 交易服务 (mall-trade)
- `POST /cart/add` - 添加购物车
- `PUT /cart/quantity` - 更新购物车数量
- `POST /planting-plan/create` - 创建种植计划
- `POST /planting-plan/{planId}/match` - 执行供给匹配

## 配置要求

### Redis配置
确保各服务的`application.yml`中配置了Redis：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: your-password  # 可选
```

### 依赖引入
`mall-common`模块已包含必要的依赖：
- `spring-boot-starter-data-redis`
- `spring-boot-starter-aop`

## 监控与日志

### 日志级别
- **INFO**: 限流触发时会记录警告日志
- **DEBUG**: 限流通过时会记录调试日志

### 关键日志
```
# 限流拒绝
WARN  - 限流触发: key=login:123, window=60s, maxRequests=50
WARN  - 限流拒绝: userId=123, method=weChatLogin, window=60s, maxRequests=50

# 限流通过
DEBUG - 限流通过: userId=123, method=addToCart, key=cart_add:123

# Redis故障降级
ERROR - 限流检查失败: key=login:123, 降级为允许访问
```

## 性能特点

1. **高性能**: 基于Redis内存操作，单次检查耗时 < 1ms
2. **准确性**: 滑动窗口算法，比固定窗口更精确
3. **原子性**: Lua脚本保证操作原子性，无并发问题
4. **可扩展**: 支持分布式部署，多实例共享限流状态
5. **容错性**: Redis故障时自动降级，不影响业务

## 测试

运行单元测试验证限流功能：

```bash
cd mall-common
mvn test -Dtest=RateLimiterTest
```

**测试用例包括:**
- 正常请求（未超限）
- 超限请求（应被拒绝）
- 滑动窗口（窗口外的记录会被清除）
- 多用户隔离
- 清空限流记录
- 实际场景（1分钟50次）

## 注意事项

1. **未登录用户**: 限流切面会跳过未登录用户的限流检查
2. **用户隔离**: 不同用户的限流互不影响
3. **Redis连接**: 确保Redis服务可用，否则会降级为允许访问
4. **时间精度**: 基于毫秒级时间戳，精度足够高
5. **内存占用**: 每个限流key在Redis中占用约 50-100 字节

## 未来优化

1. 支持基于IP的限流（未登录用户）
2. 支持动态调整限流参数
3. 提供限流监控面板
4. 支持多级限流策略
5. 集成Sentinel等成熟限流框架
