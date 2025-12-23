package xyh.dp.mall.trade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.service.HotProductService;
import xyh.dp.mall.trade.vo.HotProductVO;

import java.util.List;

/**
 * 热销商品控制器
 * 提供热销排行榜查询接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/hot-product")
@RequiredArgsConstructor
@Tag(name = "热销商品管理", description = "热销商品排行榜相关接口")
public class HotProductController {

    private final HotProductService hotProductService;

    /**
     * 查询一周内的热销商品排行（前10）
     * 
     * @return 热销商品列表
     */
    @GetMapping("/weekly")
    @Operation(summary = "一周热销排行", description = "查询最近7天的热销商品排行榜（前10）")
    public Result<List<HotProductVO>> getWeeklyHotProducts() {
        log.info("查询一周热销商品排行");
        List<HotProductVO> list = hotProductService.getWeeklyHotProducts();
        return Result.success(list);
    }

    /**
     * 查询一周内的热销商品排行（自定义数量）
     * 
     * @param topN 返回前N个商品
     * @return 热销商品列表
     */
    @GetMapping("/weekly/top/{topN}")
    @Operation(summary = "一周热销排行（自定义数量）", description = "查询最近7天的热销商品排行榜，可自定义返回数量")
    public Result<List<HotProductVO>> getWeeklyHotProductsTopN(
            @Parameter(description = "返回前N个商品") @PathVariable Integer topN
    ) {
        log.info("查询一周热销商品排行, topN={}", topN);
        
        if (topN <= 0 || topN > 100) {
            return Result.error("topN参数必须在1-100之间");
        }
        
        List<HotProductVO> list = hotProductService.getWeeklyHotProducts(topN);
        return Result.success(list);
    }

    /**
     * 查询指定分类的一周热销商品
     * 
     * @param categoryId 分类ID
     * @param topN 返回前N个商品
     * @return 热销商品列表
     */
    @GetMapping("/weekly/category/{categoryId}")
    @Operation(summary = "分类热销排行", description = "查询指定分类的一周热销商品")
    public Result<List<HotProductVO>> getWeeklyHotProductsByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "返回前N个商品") @RequestParam(defaultValue = "10") Integer topN
    ) {
        log.info("查询分类热销商品, categoryId={}, topN={}", categoryId, topN);
        
        if (topN <= 0 || topN > 100) {
            return Result.error("topN参数必须在1-100之间");
        }
        
        List<HotProductVO> list = hotProductService.getWeeklyHotProductsByCategory(categoryId, topN);
        return Result.success(list);
    }
}
