package xyh.dp.mall.gateway.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IP限流器测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootTest
class IpRateLimiterTest {
    
    @Autowired
    private IpRateLimiter ipRateLimiter;
    
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;
    
    private static final String TEST_IP = "192.168.1.100";
    private static final int WINDOW = 60;
    private static final int MAX_REQUESTS = 10;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        ipRateLimiter.reset(TEST_IP).block();
    }
    
    /**
     * 测试基本限流功能
     * 前10次请求应该全部通过，第11次应该被拒绝
     */
    @Test
    void testBasicRateLimit() {
        // 前10次请求应该成功
        for (int i = 0; i < MAX_REQUESTS; i++) {
            StepVerifier.create(ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS))
                    .expectNext(true)
                    .verifyComplete();
        }
        
        // 第11次请求应该被限流
        StepVerifier.create(ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS))
                .expectNext(false)
                .verifyComplete();
    }
    
    /**
     * 测试剩余配额查询
     */
    @Test
    void testGetRemainingQuota() {
        // 初始配额应该等于最大请求数
        StepVerifier.create(ipRateLimiter.getRemainingQuota(TEST_IP, WINDOW, MAX_REQUESTS))
                .assertNext(remaining -> assertThat(remaining).isEqualTo(MAX_REQUESTS))
                .verifyComplete();
        
        // 消费5次后，剩余配额应该是5
        for (int i = 0; i < 5; i++) {
            ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS).block();
        }
        
        StepVerifier.create(ipRateLimiter.getRemainingQuota(TEST_IP, WINDOW, MAX_REQUESTS))
                .assertNext(remaining -> assertThat(remaining).isEqualTo(5L))
                .verifyComplete();
    }
    
    /**
     * 测试重置功能
     */
    @Test
    void testReset() {
        // 先消费所有配额
        for (int i = 0; i < MAX_REQUESTS; i++) {
            ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS).block();
        }
        
        // 此时应该被限流
        StepVerifier.create(ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS))
                .expectNext(false)
                .verifyComplete();
        
        // 重置后应该恢复
        StepVerifier.create(ipRateLimiter.reset(TEST_IP))
                .expectNext(true)
                .verifyComplete();
        
        // 重置后应该可以再次请求
        StepVerifier.create(ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS))
                .expectNext(true)
                .verifyComplete();
    }
    
    /**
     * 测试并发场景
     */
    @Test
    void testConcurrentRequests() {
        List<Boolean> results = new ArrayList<>();
        
        // 模拟20个并发请求
        for (int i = 0; i < 20; i++) {
            Boolean result = ipRateLimiter.tryAcquire(TEST_IP, WINDOW, MAX_REQUESTS).block();
            results.add(result);
        }
        
        // 应该有10个通过，10个被拒绝
        long allowedCount = results.stream().filter(allowed -> allowed).count();
        long deniedCount = results.stream().filter(allowed -> !allowed).count();
        
        assertThat(allowedCount).isEqualTo(MAX_REQUESTS);
        assertThat(deniedCount).isEqualTo(10);
    }
    
    /**
     * 测试不同IP互不影响
     */
    @Test
    void testDifferentIps() {
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";
        
        // IP1消费所有配额
        for (int i = 0; i < MAX_REQUESTS; i++) {
            ipRateLimiter.tryAcquire(ip1, WINDOW, MAX_REQUESTS).block();
        }
        
        // IP1应该被限流
        StepVerifier.create(ipRateLimiter.tryAcquire(ip1, WINDOW, MAX_REQUESTS))
                .expectNext(false)
                .verifyComplete();
        
        // IP2应该不受影响
        StepVerifier.create(ipRateLimiter.tryAcquire(ip2, WINDOW, MAX_REQUESTS))
                .expectNext(true)
                .verifyComplete();
        
        // 清理
        ipRateLimiter.reset(ip1).block();
        ipRateLimiter.reset(ip2).block();
    }
}
