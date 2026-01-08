package xyh.dp.mall.common.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * QPS监控器
 * 基于Redis实现QPS统计和监控
 * 
 * <p>功能特性:
 * - 实时QPS统计（每秒请求数）
 * - 峰值QPS记录
 * - 时间窗口内QPS统计
 * - 自动过期清理
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QpsMonitor {

    private final StringRedisTemplate redisTemplate;
    
    private static final String QPS_PREFIX = "monitor:qps:";
    private static final String QPS_PEAK_PREFIX = "monitor:qps:peak:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录一次请求
     * 
     * @param endpoint 接口端点（如：/product/page）
     */
    public void recordRequest(String endpoint) {
        String key = buildKey(endpoint);
        try {
            // 增加计数
            Long count = redisTemplate.opsForValue().increment(key);
            
            // 设置过期时间为1秒（每秒重新计数）
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            
            // 更新峰值QPS
            updatePeakQps(endpoint, count);
            
        } catch (Exception e) {
            log.error("QPS记录失败: endpoint={}", endpoint, e);
        }
    }

    /**
     * 获取当前QPS
     * 
     * @param endpoint 接口端点
     * @return 当前QPS（每秒请求数）
     */
    public long getCurrentQps(String endpoint) {
        try {
            String key = buildKey(endpoint);
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.error("获取QPS失败: endpoint={}", endpoint, e);
            return 0L;
        }
    }

    /**
     * 获取峰值QPS
     * 
     * @param endpoint 接口端点
     * @return 峰值QPS
     */
    public long getPeakQps(String endpoint) {
        try {
            String key = buildPeakKey(endpoint);
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.error("获取峰值QPS失败: endpoint={}", endpoint, e);
            return 0L;
        }
    }

    /**
     * 更新峰值QPS
     * 
     * @param endpoint 接口端点
     * @param currentQps 当前QPS
     */
    private void updatePeakQps(String endpoint, Long currentQps) {
        try {
            String peakKey = buildPeakKey(endpoint);
            String peakValue = redisTemplate.opsForValue().get(peakKey);
            long peakQps = peakValue != null ? Long.parseLong(peakValue) : 0L;
            
            if (currentQps > peakQps) {
                redisTemplate.opsForValue().set(peakKey, String.valueOf(currentQps), 24, TimeUnit.HOURS);
                log.info("QPS峰值更新: endpoint={}, oldPeak={}, newPeak={}, time={}", 
                        endpoint, peakQps, currentQps, LocalDateTime.now().format(FORMATTER));
            }
        } catch (Exception e) {
            log.error("更新峰值QPS失败: endpoint={}", endpoint, e);
        }
    }

    /**
     * 重置峰值QPS
     * 
     * @param endpoint 接口端点
     */
    public void resetPeakQps(String endpoint) {
        try {
            String peakKey = buildPeakKey(endpoint);
            redisTemplate.delete(peakKey);
            log.info("重置峰值QPS: endpoint={}", endpoint);
        } catch (Exception e) {
            log.error("重置峰值QPS失败: endpoint={}", endpoint, e);
        }
    }

    /**
     * 构建Redis键
     * 
     * @param endpoint 接口端点
     * @return Redis键
     */
    private String buildKey(String endpoint) {
        return QPS_PREFIX + endpoint + ":" + (System.currentTimeMillis() / 1000);
    }

    /**
     * 构建峰值Redis键
     * 
     * @param endpoint 接口端点
     * @return Redis键
     */
    private String buildPeakKey(String endpoint) {
        return QPS_PEAK_PREFIX + endpoint;
    }
}
