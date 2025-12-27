package xyh.dp.mall.common.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.annotation.RateLimit;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.ratelimit.RateLimiter;

import java.lang.reflect.Method;

/**
 * 限流切面
 * 拦截带有@RateLimit注解的方法，实现限流控制
 * 
 * <p>限流逻辑：
 * 1. 从UserContext获取当前用户ID
 * 2. 构建限流key: prefix:userId
 * 3. 调用RateLimiter检查是否允许访问
 * 4. 如果超限，抛出BusinessException
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1) // 设置为最高优先级，在其他切面之前执行
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    /**
     * 定义切点：所有带有@RateLimit注解的方法
     */
    @Pointcut("@annotation(xyh.dp.mall.common.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }

    /**
     * 环绕通知：执行限流检查
     * 
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 如果方法执行失败或限流触发
     */
    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取@RateLimit注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            log.warn("限流检查跳过：未登录用户，方法={}", method.getName());
            // 未登录用户不进行限流（或者可以基于IP限流）
            return joinPoint.proceed();
        }

        // 构建限流key
        String key = buildRateLimitKey(rateLimit.prefix(), userId);
        
        // 检查是否允许访问
        boolean allowed = rateLimiter.tryAcquire(key, rateLimit.window(), rateLimit.maxRequests());
        
        if (!allowed) {
            log.warn("限流拒绝: userId={}, method={}, window={}s, maxRequests={}", 
                    userId, method.getName(), rateLimit.window(), rateLimit.maxRequests());
            throw new BusinessException(rateLimit.message());
        }

        // 允许访问，执行方法
        log.debug("限流通过: userId={}, method={}, key={}", userId, method.getName(), key);
        return joinPoint.proceed();
    }

    /**
     * 构建限流key
     * 格式: prefix:userId
     * 
     * @param prefix key前缀
     * @param userId 用户ID
     * @return 限流key
     */
    private String buildRateLimitKey(String prefix, Long userId) {
        return prefix + ":" + userId;
    }
}
