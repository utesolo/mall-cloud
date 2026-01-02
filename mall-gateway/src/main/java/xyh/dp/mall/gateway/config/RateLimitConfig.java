package xyh.dp.mall.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IP限流配置
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.ratelimit")
public class RateLimitConfig {
    
    /**
     * 是否启用限流
     */
    private Boolean enabled = true;
    
    /**
     * 时间窗口（秒）
     */
    private Integer window = 60;
    
    /**
     * 最大请求数
     */
    private Integer maxRequests = 100;
    
    /**
     * 限流提示信息
     */
    private String message = "请求过于频繁，请稍后再试";
    
    /**
     * IP白名单，这些IP不受限流限制
     */
    private String[] whitelist = {"127.0.0.1", "0:0:0:0:0:0:0:1"};
}
