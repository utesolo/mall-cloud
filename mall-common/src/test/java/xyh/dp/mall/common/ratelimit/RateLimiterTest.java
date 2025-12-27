package xyh.dp.mall.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流器测试
 * 测试基于Redis的滑动窗口限流功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest(classes = {RedisAutoConfiguration.class, RateLimiter.class})
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.database=15"  // 使用独立的测试数据库
})
class RateLimiterTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_KEY = "test:rate_limit:user123";

    /**
     * 每个测试前清理测试数据
     */
    @BeforeEach
    void setUp() {
        rateLimiter.clear(TEST_KEY);
    }

    /**
     * 每个测试后清理测试数据
     */
    @AfterEach
    void tearDown() {
        rateLimiter.clear(TEST_KEY);
    }

    /**
     * 测试正常请求（未超限）
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testNormalRequest() {
        // 允许5次请求，窗口60秒
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimiter.tryAcquire(TEST_KEY, 60, 5);
            assertTrue(allowed, "第" + (i + 1) + "次请求应该被允许");
        }

        // 验证计数正确
        long count = rateLimiter.getCurrentCount(TEST_KEY, 60);
        assertEquals(5, count);
    }

    /**
     * 测试超限请求（应被拒绝）
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testRateLimitExceeded() {
        // 允许3次请求
        for (int i = 0; i < 3; i++) {
            boolean allowed = rateLimiter.tryAcquire(TEST_KEY, 60, 3);
            assertTrue(allowed, "前3次请求应该被允许");
        }

        // 第4次请求应该被拒绝
        boolean allowed = rateLimiter.tryAcquire(TEST_KEY, 60, 3);
        assertFalse(allowed, "第4次请求应该被拒绝");

        // 第5次请求也应该被拒绝
        allowed = rateLimiter.tryAcquire(TEST_KEY, 60, 3);
        assertFalse(allowed, "第5次请求应该被拒绝");
    }

    /**
     * 测试滑动窗口（窗口外的记录会被清除）
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testSlidingWindow() throws InterruptedException {
        // 使用2秒窗口，最多3次请求
        int window = 2;
        int maxRequests = 3;

        // 前3次请求成功
        for (int i = 0; i < 3; i++) {
            boolean allowed = rateLimiter.tryAcquire(TEST_KEY, window, maxRequests);
            assertTrue(allowed, "前3次请求应该被允许");
        }

        // 第4次立即请求应该被拒绝
        boolean allowed = rateLimiter.tryAcquire(TEST_KEY, window, maxRequests);
        assertFalse(allowed, "第4次请求应该被拒绝");

        // 等待2.1秒，窗口滑动后旧记录应该被清除
        log.info("等待{}秒，让窗口滑动...", window);
        Thread.sleep((window + 1) * 1000L);

        // 窗口滑动后，新请求应该被允许
        allowed = rateLimiter.tryAcquire(TEST_KEY, window, maxRequests);
        assertTrue(allowed, "窗口滑动后的请求应该被允许");
    }

    /**
     * 测试不同用户的限流互不影响
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testMultipleUsers() {
        String user1Key = "test:rate_limit:user1";
        String user2Key = "test:rate_limit:user2";

        try {
            // 用户1的3次请求
            for (int i = 0; i < 3; i++) {
                boolean allowed = rateLimiter.tryAcquire(user1Key, 60, 3);
                assertTrue(allowed, "用户1的请求应该被允许");
            }

            // 用户1的第4次请求被拒绝
            boolean allowed = rateLimiter.tryAcquire(user1Key, 60, 3);
            assertFalse(allowed, "用户1的第4次请求应该被拒绝");

            // 用户2的请求不受影响
            for (int i = 0; i < 3; i++) {
                allowed = rateLimiter.tryAcquire(user2Key, 60, 3);
                assertTrue(allowed, "用户2的请求应该被允许");
            }

        } finally {
            // 清理测试数据
            rateLimiter.clear(user1Key);
            rateLimiter.clear(user2Key);
        }
    }

    /**
     * 测试获取当前计数
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testGetCurrentCount() {
        // 初始计数为0
        long count = rateLimiter.getCurrentCount(TEST_KEY, 60);
        assertEquals(0, count);

        // 执行3次请求
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire(TEST_KEY, 60, 5);
        }

        // 验证计数为3
        count = rateLimiter.getCurrentCount(TEST_KEY, 60);
        assertEquals(3, count);
    }

    /**
     * 测试清空限流记录
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testClear() {
        // 执行3次请求
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire(TEST_KEY, 60, 3);
        }

        // 验证计数为3
        long count = rateLimiter.getCurrentCount(TEST_KEY, 60);
        assertEquals(3, count);

        // 清空记录
        rateLimiter.clear(TEST_KEY);

        // 验证计数为0
        count = rateLimiter.getCurrentCount(TEST_KEY, 60);
        assertEquals(0, count);

        // 清空后新请求应该被允许
        boolean allowed = rateLimiter.tryAcquire(TEST_KEY, 60, 3);
        assertTrue(allowed, "清空后的请求应该被允许");
    }

    /**
     * 测试1分钟50次的实际场景
     * 
     * @throws Exception 测试异常
     */
    @Test
    void testRealScenario() {
        int window = 60;  // 60秒
        int maxRequests = 50;  // 50次

        // 执行50次请求，应该全部成功
        for (int i = 0; i < maxRequests; i++) {
            boolean allowed = rateLimiter.tryAcquire(TEST_KEY, window, maxRequests);
            assertTrue(allowed, "前" + maxRequests + "次请求应该被允许");
        }

        // 第51次请求应该被拒绝
        boolean allowed = rateLimiter.tryAcquire(TEST_KEY, window, maxRequests);
        assertFalse(allowed, "第51次请求应该被拒绝");

        // 验证计数
        long count = rateLimiter.getCurrentCount(TEST_KEY, window);
        assertEquals(maxRequests, count);
    }
}
