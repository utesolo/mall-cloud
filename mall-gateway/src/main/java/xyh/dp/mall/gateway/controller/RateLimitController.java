package xyh.dp.mall.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import xyh.dp.mall.gateway.config.RateLimitConfig;
import xyh.dp.mall.gateway.ratelimit.IpRateLimiter;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流管理控制器
 * 提供限流状态查询和管理功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/gateway/ratelimit")
@RequiredArgsConstructor
public class RateLimitController {
    
    private final IpRateLimiter ipRateLimiter;
    private final RateLimitConfig rateLimitConfig;
    
    /**
     * 获取限流配置信息
     * 
     * @return 限流配置
     */
    @GetMapping("/config")
    public Mono<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", rateLimitConfig.getEnabled());
        config.put("window", rateLimitConfig.getWindow());
        config.put("maxRequests", rateLimitConfig.getMaxRequests());
        config.put("message", rateLimitConfig.getMessage());
        config.put("whitelist", rateLimitConfig.getWhitelist());
        
        return Mono.just(config);
    }
    
    /**
     * 查询指定IP的剩余配额
     * 
     * @param ip IP地址
     * @return 剩余配额信息
     */
    @GetMapping("/quota/{ip}")
    public Mono<Map<String, Object>> getQuota(@PathVariable String ip) {
        return ipRateLimiter.getRemainingQuota(
                ip,
                rateLimitConfig.getWindow(),
                rateLimitConfig.getMaxRequests()
        ).map(remaining -> {
            Map<String, Object> result = new HashMap<>();
            result.put("ip", ip);
            result.put("maxRequests", rateLimitConfig.getMaxRequests());
            result.put("remaining", remaining);
            result.put("window", rateLimitConfig.getWindow());
            result.put("used", rateLimitConfig.getMaxRequests() - remaining);
            return result;
        });
    }
    
    /**
     * 重置指定IP的限流计数
     * 
     * @param ip IP地址
     * @return 重置结果
     */
    @DeleteMapping("/reset/{ip}")
    public Mono<Map<String, Object>> reset(@PathVariable String ip) {
        return ipRateLimiter.reset(ip).map(success -> {
            Map<String, Object> result = new HashMap<>();
            result.put("ip", ip);
            result.put("success", success);
            result.put("message", success ? "重置成功" : "重置失败");
            
            if (success) {
                log.info("管理员重置IP限流: ip={}", ip);
            }
            
            return result;
        });
    }
    
    /**
     * 测试IP限流
     * 
     * @param ip IP地址
     * @return 测试结果
     */
    @GetMapping("/test/{ip}")
    public Mono<Map<String, Object>> test(@PathVariable String ip) {
        return ipRateLimiter.tryAcquire(
                ip,
                rateLimitConfig.getWindow(),
                rateLimitConfig.getMaxRequests()
        ).flatMap(allowed -> 
            ipRateLimiter.getRemainingQuota(
                    ip,
                    rateLimitConfig.getWindow(),
                    rateLimitConfig.getMaxRequests()
            ).map(remaining -> {
                Map<String, Object> result = new HashMap<>();
                result.put("ip", ip);
                result.put("allowed", allowed);
                result.put("remaining", remaining);
                result.put("message", allowed ? "允许访问" : "触发限流");
                return result;
            })
        );
    }
}
