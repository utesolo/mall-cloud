package xyh.dp.mall.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.annotation.RequireLogin;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.service.PurchaseRecordService;
import xyh.dp.mall.trade.vo.PurchaseRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 购买记录控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/purchase-record")
@RequiredArgsConstructor
@Tag(name = "购买记录接口", description = "查询购买历史、购买统计等")
public class PurchaseRecordController {
    
    private final PurchaseRecordService purchaseRecordService;
    
    /**
     * 分页查询我的购买记录
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 购买记录分页数据
     */
    @GetMapping("/my-records")
    @RequireLogin
    @Operation(summary = "查询我的购买记录", description = "分页查询当前用户的购买历史")
    public Result<Page<PurchaseRecordVO>> getMyRecords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        log.info("查询购买记录请求: pageNum={}, pageSize={}", pageNum, pageSize);
        Page<PurchaseRecordVO> page = purchaseRecordService.pageMyRecords(pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 查询我的购买统计
     * 
     * @return 购买统计数据
     */
    @GetMapping("/my-statistics")
    @RequireLogin
    @Operation(summary = "查询购买统计", description = "获取当前用户的购买统计信息")
    public Result<Map<String, Object>> getMyStatistics() {
        log.info("查询购买统计");
        Map<String, Object> statistics = purchaseRecordService.getMyStatistics();
        return Result.success(statistics);
    }
    
    /**
     * 检查是否购买过某商品
     * 
     * @param productId 商品ID
     * @return 是否购买过
     */
    @GetMapping("/has-purchased/{productId}")
    @RequireLogin
    @Operation(summary = "检查是否购买过", description = "查询用户是否购买过指定商品")
    public Result<Boolean> hasPurchased(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("检查是否购买过: productId={}", productId);
        boolean hasPurchased = purchaseRecordService.hasPurchased(productId);
        return Result.success(hasPurchased);
    }
    
    /**
     * 查询购买过的商品ID列表
     * 
     * @return 商品ID列表
     */
    @GetMapping("/purchased-product-ids")
    @RequireLogin
    @Operation(summary = "查询购买过的商品", description = "获取用户购买过的所有商品ID")
    public Result<List<Long>> getPurchasedProductIds() {
        log.info("查询购买过的商品ID列表");
        List<Long> productIds = purchaseRecordService.getMyPurchasedProductIds();
        return Result.success(productIds);
    }
}
