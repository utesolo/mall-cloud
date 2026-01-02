package xyh.dp.mall.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyh.dp.mall.gateway.config.RateLimitConfig;
import xyh.dp.mall.gateway.ratelimit.IpRateLimiter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * IP限流全局过滤器
 * 在网关层面对所有请求进行IP限流控制
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimitFilter implements GlobalFilter, Ordered {
    
    private final IpRateLimiter ipRateLimiter;
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查是否启用限流
        if (!rateLimitConfig.getEnabled()) {
            return chain.filter(exchange);
        }
        
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // 跳过健康检查等路径
        if (shouldSkip(path)) {
            return chain.filter(exchange);
        }
        
        // 获取真实IP
        String ip = getRealIp(request);
        
        // 检查IP白名单
        if (isWhitelisted(ip)) {
            log.debug("IP在白名单中，跳过限流: ip={}", ip);
            return chain.filter(exchange);
        }
        
        // 执行限流检查
        return ipRateLimiter.tryAcquire(
                ip,
                rateLimitConfig.getWindow(),
                rateLimitConfig.getMaxRequests()
        )
        .flatMap(allowed -> {
            if (allowed) {
                // 添加限流信息到响应头
                return addRateLimitHeaders(exchange, ip)
                        .then(chain.filter(exchange));
            } else {
                // 触发限流，返回429错误
                return handleRateLimitExceeded(exchange, ip);
            }
        });
    }
    
    /**
     * 获取真实IP地址
     * 优先从代理头中获取
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getRealIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        
        // 尝试从X-Forwarded-For获取
        String ip = headers.getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            }
            return ip.trim();
        }
        
        // 尝试从X-Real-IP获取
        ip = headers.getFirst("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        // 尝试从Proxy-Client-IP获取
        ip = headers.getFirst("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        // 尝试从WL-Proxy-Client-IP获取
        ip = headers.getFirst("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        
        // 从远程地址获取
        if (request.getRemoteAddress() != null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return ip != null ? ip : "unknown";
    }
    
    /**
     * 检查IP是否在白名单中
     * 
     * @param ip IP地址
     * @return 是否在白名单
     */
    private boolean isWhitelisted(String ip) {
        String[] whitelist = rateLimitConfig.getWhitelist();
        if (whitelist == null || whitelist.length == 0) {
            return false;
        }
        return Arrays.asList(whitelist).contains(ip);
    }
    
    /**
     * 判断是否跳过限流检查
     * 
     * @param path 请求路径
     * @return 是否跳过
     */
    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/health") ||
               path.equals("/favicon.ico");
    }
    
    /**
     * 添加限流相关的响应头
     * 
     * @param exchange 交换对象
     * @param ip IP地址
     * @return Mono<Void>
     */
    private Mono<Void> addRateLimitHeaders(ServerWebExchange exchange, String ip) {
        return ipRateLimiter.getRemainingQuota(
                ip,
                rateLimitConfig.getWindow(),
                rateLimitConfig.getMaxRequests()
        )
        .doOnNext(remaining -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            headers.add("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getMaxRequests()));
            headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
            headers.add("X-RateLimit-Window", rateLimitConfig.getWindow() + "s");
        })
        .then();
    }
    
    /**
     * 处理限流触发的情况
     * 
     * @param exchange 交换对象
     * @param ip IP地址
     * @return Mono<Void>
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String ip) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 添加限流响应头
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getMaxRequests()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Window", rateLimitConfig.getWindow() + "s");
        response.getHeaders().add("Retry-After", String.valueOf(rateLimitConfig.getWindow()));
        
        // 构造错误响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", rateLimitConfig.getMessage());
        result.put("data", null);
        
        try {
            String json = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            
            log.warn("IP限流触发，拒绝请求: ip={}, path={}", ip, exchange.getRequest().getPath());
            
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("构造限流响应失败", e);
            return response.setComplete();
        }
    }
    
    /**
     * 设置过滤器优先级
     * 数值越小，优先级越高
     * 在认证过滤器之前执行
     * 
     * @return 优先级
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
