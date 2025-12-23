package xyh.dp.mall.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.search.service.ProductSyncService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品数据同步控制器
 * 提供手动触发数据同步的接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "商品数据同步", description = "MySQL到ES的数据同步接口")
public class ProductSyncController {
    
    private final ProductSyncService productSyncService;
    
    /**
     * 全量同步所有商品
     * 
     * @return 同步结果
     */
    @PostMapping("/all")
    @Operation(summary = "全量同步", description = "从MySQL全量同步商品数据到ES，会先清空ES数据")
    public Result<Map<String, Object>> syncAll() {
        log.info("触发全量同步");
        
        long startTime = System.currentTimeMillis();
        int count = productSyncService.syncAll();
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("duration", endTime - startTime);
        result.put("message", "全量同步完成");
        
        return Result.success(result);
    }
    
    /**
     * 增量同步单个商品
     * 
     * @param productId 商品ID
     * @return 同步结果
     */
    @PostMapping("/one/{productId}")
    @Operation(summary = "同步单个商品", description = "增量同步指定商品到ES")
    public Result<String> syncOne(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("触发增量同步: {}", productId);
        
        boolean success = productSyncService.syncOne(productId);
        return success ? Result.success("同步成功") : Result.error("同步失败");
    }
    
    /**
     * 批量同步商品
     * 
     * @param productIds 商品ID列表
     * @return 同步结果
     */
    @PostMapping("/batch")
    @Operation(summary = "批量同步", description = "批量同步指定商品到ES")
    public Result<Map<String, Object>> syncBatch(@RequestBody List<Long> productIds) {
        log.info("触发批量同步，数量: {}", productIds.size());
        
        int successCount = productSyncService.syncBatch(productIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", productIds.size());
        result.put("success", successCount);
        result.put("failed", productIds.size() - successCount);
        
        return Result.success(result);
    }
    
    /**
     * 删除商品
     * 
     * @param productId 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "删除商品", description = "从ES中删除指定商品")
    public Result<String> deleteOne(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("触发商品删除: {}", productId);
        
        boolean success = productSyncService.deleteOne(productId);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
    
    /**
     * 查询ES商品数量
     * 
     * @return 商品数量
     */
    @GetMapping("/count")
    @Operation(summary = "查询ES商品数量", description = "获取ES中的商品总数")
    public Result<Map<String, Object>> count() {
        long count = productSyncService.count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        
        return Result.success(result);
    }
    
    /**
     * 检查商品是否存在
     * 
     * @param productId 商品ID
     * @return 是否存在
     */
    @GetMapping("/exists/{productId}")
    @Operation(summary = "检查商品是否存在", description = "检查指定商品是否在ES中")
    public Result<Map<String, Object>> exists(@Parameter(description = "商品ID") @PathVariable Long productId) {
        boolean exists = productSyncService.exists(productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("exists", exists);
        
        return Result.success(result);
    }
}

