package xyh.dp.mall.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 限流器
 * 基于Redis实现滑动窗口限流
 * 
 * <p>使用Lua脚本保证原子性操作：
 * 1. 移除窗口外的旧记录
 * 2. 统计窗口内的请求次数
 * 3. 如果未超限，则添加新记录
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua脚本：滑动窗口限流
     * 
     * KEYS[1]: 限流key
     * ARGV[1]: 当前时间戳(毫秒)
     * ARGV[2]: 时间窗口(毫秒)
     * ARGV[3]: 最大请求次数
     * 
     * 返回值：
     * - 1: 允许请求
     * - 0: 拒绝请求(超过限制)
     */
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local max_requests = tonumber(ARGV[3])
            
            -- 窗口开始时间
            local window_start = now - window
            
            -- 移除窗口外的旧记录
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            
            -- 统计窗口内的请求次数
            local current_count = redis.call('ZCARD', key)
            
            -- 检查是否超限
            if current_count < max_requests then
                -- 未超限，添加新记录
                redis.call('ZADD', key, now, now)
                -- 设置key过期时间（窗口时间+1秒）
                redis.call('EXPIRE', key, math.ceil(window / 1000) + 1)
                return 1
            else
                -- 已超限
                return 0
            end
            """;

    /**
     * 尝试获取访问许可
     * 
     * @param key 限流key
     * @param window 时间窗口（秒）
     * @param maxRequests 最大请求次数
     * @return true-允许访问，false-拒绝访问
     * @throws RuntimeException 如果Redis操作失败
     */
    public boolean tryAcquire(String key, int window, int maxRequests) {
        try {
            long now = System.currentTimeMillis();
            long windowMillis = window * 1000L;

            // 执行Lua脚本
            RedisScript<Long> script = RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);
            List<String> keys = Collections.singletonList(key);
            Long result = redisTemplate.execute(
                    script,
                    keys,
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(maxRequests)
            );

            boolean allowed = result != null && result == 1;
            
            if (!allowed) {
                log.warn("限流触发: key={}, window={}s, maxRequests={}", key, window, maxRequests);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("限流检查失败: key={}, 降级为允许访问", key, e);
            // 降级策略：Redis故障时允许访问
            return true;
        }
    }

    /**
     * 获取当前窗口内的请求次数
     * 
     * @param key 限流key
     * @param window 时间窗口（秒）
     * @return 请求次数
     */
    public long getCurrentCount(String key, int window) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - (window * 1000L);
            
            // 先移除过期数据
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            
            // 统计窗口内的请求次数
            Long count = redisTemplate.opsForZSet().zCard(key);
            return count != null ? count : 0;
            
        } catch (Exception e) {
            log.error("获取限流计数失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 清空限流记录
     * 
     * @param key 限流key
     */
    public void clear(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("清空限流记录: key={}", key);
        } catch (Exception e) {
            log.error("清空限流记录失败: key={}", key, e);
        }
    }
}
