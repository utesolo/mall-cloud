package xyh.dp.mall.job.task;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.job.feign.TradeFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 热销商品榜单缓存更新任务
 * 定期更新热销商品排行榜到Redis缓存
 * 
 * <p>执行策略：
 * - 每小时更新一次热销榜单缓存
 * - 缓存有效期2小时（防止任务失败时数据过期）
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotProductCacheTask {

    private final TradeFeignClient tradeFeignClient;
    private final StringRedisTemplate redisTemplate;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CACHE_KEY_PREFIX = "hot_products:weekly:top_";
    private static final long CACHE_EXPIRE_HOURS = 2;

    /**
     * 更新热销商品榜单缓存
     * 每小时执行一次
     * 
     * Cron表达式说明：0 0 * * * ?
     * - 秒：0
     * - 分：0
     * - 时：*（每小时）
     * - 日：*（每天）
     * - 月：*（每月）
     * - 星期：?（不指定）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshHotProductCache() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("================== 开始执行热销榜单缓存更新任务 ==================");
        log.info("任务开始时间: {}", startTime);
        
        try {
            long start = System.currentTimeMillis();
            
            // 更新TOP10
            updateHotProductsCache(10);
            
            // 更新TOP20（用于首页展示更多商品）
            updateHotProductsCache(20);
            
            // 更新TOP50（用于数据分析）
            updateHotProductsCache(50);
            
            long duration = System.currentTimeMillis() - start;
            String endTime = LocalDateTime.now().format(FORMATTER);
            
            log.info("热销榜单缓存更新成功");
            log.info("耗时: {} 毫秒", duration);
            log.info("任务结束时间: {}", endTime);
            
        } catch (Exception e) {
            log.error("热销榜单缓存更新异常", e);
        } finally {
            log.info("================== 热销榜单缓存更新任务执行完毕 ==================");
        }
    }

    /**
     * 更新指定数量的热销商品缓存
     * 
     * @param topN 前N个商品
     */
    private void updateHotProductsCache(int topN) {
        try {
            log.info("开始更新TOP{}热销商品缓存", topN);
            
            // 调用交易服务获取热销商品
            Result<List<Object>> result = tradeFeignClient.getWeeklyHotProducts(topN);
            
            if (!result.isSuccess() || result.getData() == null) {
                log.warn("获取TOP{}热销商品失败: {}", topN, result.getMessage());
                return;
            }
            
            List<Object> hotProducts = result.getData();
            
            // 将结果缓存到Redis
            String cacheKey = CACHE_KEY_PREFIX + topN;
            String cacheValue = JSON.toJSONString(hotProducts);
            
            redisTemplate.opsForValue().set(
                    cacheKey,
                    cacheValue,
                    CACHE_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
            
            log.info("TOP{}热销商品缓存更新成功，共{}个商品", topN, hotProducts.size());
            
        } catch (Exception e) {
            log.error("更新TOP{}热销商品缓存失败", topN, e);
        }
    }

    /**
     * 手动触发缓存清除（可扩展为API接口）
     */
    public void clearHotProductCache() {
        try {
            log.info("开始清除热销榜单缓存");
            
            // 清除各个榜单的缓存
            redisTemplate.delete(CACHE_KEY_PREFIX + "10");
            redisTemplate.delete(CACHE_KEY_PREFIX + "20");
            redisTemplate.delete(CACHE_KEY_PREFIX + "50");
            
            log.info("热销榜单缓存清除完成");
            
        } catch (Exception e) {
            log.error("清除热销榜单缓存失败", e);
        }
    }
}
