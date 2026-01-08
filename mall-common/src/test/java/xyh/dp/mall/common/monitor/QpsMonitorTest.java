package xyh.dp.mall.common.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * QPS监控器测试类
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QPS监控器测试")
class QpsMonitorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private QpsMonitor qpsMonitor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("记录请求应该增加计数并设置过期时间")
    void recordRequest_shouldIncrementAndSetExpire() {
        // Given
        String endpoint = "/product/page";
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // When
        qpsMonitor.recordRequest(endpoint);

        // Then
        verify(valueOperations).increment(anyString());
        verify(redisTemplate).expire(anyString(), eq(1L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("获取当前QPS应该返回Redis中的值")
    void getCurrentQps_shouldReturnRedisValue() {
        // Given
        String endpoint = "/product/page";
        when(valueOperations.get(anyString())).thenReturn("100");

        // When
        long qps = qpsMonitor.getCurrentQps(endpoint);

        // Then
        assertThat(qps).isEqualTo(100L);
    }

    @Test
    @DisplayName("当Redis返回null时应该返回0")
    void getCurrentQps_shouldReturnZeroWhenRedisReturnsNull() {
        // Given
        String endpoint = "/product/page";
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        long qps = qpsMonitor.getCurrentQps(endpoint);

        // Then
        assertThat(qps).isEqualTo(0L);
    }

    @Test
    @DisplayName("获取峰值QPS应该返回Redis中的峰值")
    void getPeakQps_shouldReturnPeakValue() {
        // Given
        String endpoint = "/product/page";
        when(valueOperations.get(contains("peak"))).thenReturn("500");

        // When
        long peakQps = qpsMonitor.getPeakQps(endpoint);

        // Then
        assertThat(peakQps).isEqualTo(500L);
    }

    @Test
    @DisplayName("重置峰值QPS应该删除Redis键")
    void resetPeakQps_shouldDeleteRedisKey() {
        // Given
        String endpoint = "/product/page";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        qpsMonitor.resetPeakQps(endpoint);

        // Then
        verify(redisTemplate).delete(contains("peak"));
    }

    @Test
    @DisplayName("Redis异常时记录请求不应该抛出异常")
    void recordRequest_shouldNotThrowExceptionOnRedisError() {
        // Given
        String endpoint = "/product/page";
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When & Then - 不应该抛出异常
        qpsMonitor.recordRequest(endpoint);
        
        verify(valueOperations).increment(anyString());
    }
}
