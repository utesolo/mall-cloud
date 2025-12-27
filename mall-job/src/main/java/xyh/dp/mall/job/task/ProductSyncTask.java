package xyh.dp.mall.job.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.job.feign.SearchFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ES商品数据同步定时任务
 * 负责定期将MySQL商品数据同步到Elasticsearch
 * 
 * <p>执行策略：
 * - 全量同步：每天凌晨3点执行（业务低峰期）
 * - 增量同步：每小时执行一次（暂未实现，可扩展）
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncTask {

    private final SearchFeignClient searchFeignClient;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 全量同步商品数据到ES
     * 每天凌晨3点执行
     * 
     * Cron表达式说明：0 0 3 * * ?
     * - 秒：0
     * - 分：0
     * - 时：3
     * - 日：*（每天）
     * - 月：*（每月）
     * - 星期：?（不指定）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncAllProducts() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("================== 开始执行全量同步任务 ==================");
        log.info("任务开始时间: {}", startTime);
        
        try {
            long start = System.currentTimeMillis();
            
            // 调用搜索服务的同步接口
            Result<Map<String, Object>> result = searchFeignClient.syncAll();
            
            long duration = System.currentTimeMillis() - start;
            String endTime = LocalDateTime.now().format(FORMATTER);
            
            if (result.isSuccess()) {
                Map<String, Object> data = result.getData();
                log.info("全量同步任务执行成功");
                log.info("同步结果: {}", data);
                log.info("耗时: {} 毫秒", duration);
            } else {
                log.error("全量同步任务执行失败: {}", result.getMessage());
            }
            
            log.info("任务结束时间: {}", endTime);
            
        } catch (Exception e) {
            log.error("全量同步任务执行异常", e);
        } finally {
            log.info("================== 全量同步任务执行完毕 ==================");
        }
    }

    /**
     * 增量同步商品数据到ES（预留）
     * 每小时执行一次
     * 
     * Cron表达式说明：0 0 * * * ?
     * - 秒：0
     * - 分：0
     * - 时：*（每小时）
     * - 日：*（每天）
     * - 月：*（每月）
     * - 星期：?（不指定）
     * 
     * 注意：此任务暂时注释，需要实现增量同步逻辑后再启用
     */
    // @Scheduled(cron = "0 0 * * * ?")
    public void syncIncrementalProducts() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("================== 开始执行增量同步任务 ==================");
        log.info("任务开始时间: {}", startTime);
        
        try {
            long start = System.currentTimeMillis();
            
            // 调用搜索服务的增量同步接口
            Result<Map<String, Object>> result = searchFeignClient.syncIncremental();
            
            long duration = System.currentTimeMillis() - start;
            String endTime = LocalDateTime.now().format(FORMATTER);
            
            if (result.isSuccess()) {
                Map<String, Object> data = result.getData();
                log.info("增量同步任务执行成功");
                log.info("同步结果: {}", data);
                log.info("耗时: {} 毫秒", duration);
            } else {
                log.error("增量同步任务执行失败: {}", result.getMessage());
            }
            
            log.info("任务结束时间: {}", endTime);
            
        } catch (Exception e) {
            log.error("增量同步任务执行异常", e);
        } finally {
            log.info("================== 增量同步任务执行完毕 ==================");
        }
    }
}
