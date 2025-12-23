package xyh.dp.mall.search.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.search.dto.ProductSearchDTO;
import xyh.dp.mall.search.service.ProductSearchService;
import xyh.dp.mall.search.vo.ProductSearchVO;

import java.util.List;

/**
 * 商品搜索控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "商品搜索接口", description = "提供商品全文搜索、智能推荐等功能")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * 综合搜索商品
     * 
     * @param searchDTO 搜索条件
     * @return 搜索结果
     */
    @PostMapping("/products")
    @Operation(summary = "综合搜索商品", description = "支持多条件组合搜索、全文检索、排序等功能")
    public Result<Page<ProductSearchVO>> searchProducts(@RequestBody ProductSearchDTO searchDTO) {
        log.info("商品搜索请求: {}", searchDTO);
        Page<ProductSearchVO> page = productSearchService.searchProducts(searchDTO);
        return Result.success(page);
    }

    /**
     * 根据ID查询商品
     * 
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/product/{id}")
    @Operation(summary = "根据ID查询商品", description = "查询单个商品详情")
    public Result<ProductSearchVO> getById(
            @Parameter(description = "商品ID") @PathVariable Long id
    ) {
        log.info("查询商品详情: id={}", id);
        ProductSearchVO vo = productSearchService.getById(id);
        if (vo == null) {
            return Result.error("商品不存在");
        }
        return Result.success(vo);
    }

    /**
     * 热门商品推荐
     * 
     * @param limit 返回数量
     * @return 热门商品列表
     */
    @GetMapping("/hot")
    @Operation(summary = "热门商品推荐", description = "根据销量排序返回热门商品")
    public Result<List<ProductSearchVO>> getHotProducts(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("获取热门商品: limit={}", limit);
        List<ProductSearchVO> list = productSearchService.getHotProducts(limit);
        return Result.success(list);
    }

    /**
     * 搜索建议/自动补全
     * 
     * @param keyword 关键词
     * @param limit 返回数量
     * @return 建议商品列表
     */
    @GetMapping("/suggestions")
    @Operation(summary = "搜索建议", description = "根据关键词提供搜索建议和自动补全")
    public Result<List<ProductSearchVO>> getSuggestions(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") Integer limit
    ) {
        log.info("获取搜索建议: keyword={}, limit={}", keyword, limit);
        List<ProductSearchVO> list = productSearchService.getSuggestions(keyword, limit);
        return Result.success(list);
    }
}
