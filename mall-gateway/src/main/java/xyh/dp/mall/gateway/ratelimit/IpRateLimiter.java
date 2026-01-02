package xyh.dp.mall.gateway.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * 基于Redis的限流器（响应式版本）
 * 使用滑动窗口算法实现精确限流
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimiter {
    
    private final ReactiveStringRedisTemplate redisTemplate;
    
    /**
     * Lua脚本：滑动窗口限流算法
     * 1. 删除窗口外的旧数据
     * 2. 统计窗口内的请求数
     * 3. 如果未超限，添加新请求记录
     * 4. 返回是否允许请求
     */
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local max_requests = tonumber(ARGV[3])
            
            -- 删除窗口外的旧数据
            local window_start = now - window * 1000
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            
            -- 统计窗口内的请求数
            local current_count = redis.call('ZCARD', key)
            
            if current_count < max_requests then
                -- 添加当前请求
                redis.call('ZADD', key, now, now)
                -- 设置过期时间（窗口时间 + 1秒的缓冲）
                redis.call('EXPIRE', key, window + 1)
                return 1
            else
                -- 超过限制
                return 0
            end
            """;
    
    /**
     * 尝试获取访问许可
     * 
     * @param ip IP地址
     * @param window 时间窗口（秒）
     * @param maxRequests 最大请求数
     * @return Mono<Boolean> 是否允许访问
     */
    public Mono<Boolean> tryAcquire(String ip, int window, int maxRequests) {
        String key = buildKey(ip);
        long now = System.currentTimeMillis();
        
        RedisScript<Long> script = RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);
        
        return redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(window),
                String.valueOf(maxRequests)
        )
        .next()
        .map(result -> result == 1L)
        .doOnNext(allowed -> {
            if (!allowed) {
                log.warn("IP限流触发: ip={}, window={}s, maxRequests={}", ip, window, maxRequests);
            }
        })
        .onErrorResume(e -> {
            log.error("限流检查失败，默认放行: ip={}, error={}", ip, e.getMessage());
            return Mono.just(true); // 降级策略：出错时放行
        });
    }
    
    /**
     * 获取剩余配额
     * 
     * @param ip IP地址
     * @param window 时间窗口（秒）
     * @param maxRequests 最大请求数
     * @return Mono<Long> 剩余配额
     */
    public Mono<Long> getRemainingQuota(String ip, int window, int maxRequests) {
        String key = buildKey(ip);
        long now = System.currentTimeMillis();
        double windowStart = now - window * 1000.0;
        
        return redisTemplate.opsForZSet()
                .removeRangeByScore(key, Range.closed(Double.valueOf(0.0), Double.valueOf(windowStart)))
                .then(redisTemplate.opsForZSet().count(key, Range.<Double>unbounded()))
                .map(count -> Math.max(0, maxRequests - count))
                .onErrorResume(e -> {
                    log.error("获取剩余配额失败: ip={}, error={}", ip, e.getMessage());
                    return Mono.just(0L);
                });
    }
    
    /**
     * 构建Redis键
     * 
     * @param ip IP地址
     * @return Redis键
     */
    private String buildKey(String ip) {
        return "gateway:rate_limit:ip:" + ip;
    }
    
    /**
     * 重置IP的限流计数
     * 
     * @param ip IP地址
     * @return Mono<Boolean> 是否重置成功
     */
    public Mono<Boolean> reset(String ip) {
        String key = buildKey(ip);
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.info("重置IP限流计数: ip={}", ip);
                    }
                });
    }
}
