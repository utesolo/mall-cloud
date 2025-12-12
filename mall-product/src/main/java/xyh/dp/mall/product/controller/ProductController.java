package xyh.dp.mall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.product.entity.Category;
import xyh.dp.mall.product.service.ProductService;
import xyh.dp.mall.product.vo.ProductVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "商品接口", description = "商品查询、分类查询等接口")
public class ProductController {
    // TODO 商品添加，修改，删除

    private final ProductService productService;

    /**
     * 分页查询商品列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param categoryId 分类ID
     * @param keyword 关键词
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param region 区域
     * @return 商品分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询商品", description = "支持分类、关键词、价格区间、区域筛选")
    public Result<Page<ProductVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "适配区域") @RequestParam(required = false) String region
    ) {
        Page<ProductVO> page = productService.pageQuery(pageNum, pageSize, categoryId, keyword, minPrice, maxPrice, region);
        return Result.success(page);
    }

    /**
     * 查询商品详情
     * 
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询商品详情", description = "根据商品ID查询详细信息")
    public Result<ProductVO> getById(@Parameter(description = "商品ID") @PathVariable Long id) {
        ProductVO product = productService.getById(id);
        return Result.success(product);
    }

    /**
     * 查询分类列表
     * 
     * @return 分类列表
     */
    @GetMapping("/category/list")
    @Operation(summary = "查询分类列表", description = "获取所有商品分类")
    public Result<List<Category>> getCategoryList() {
        List<Category> categoryList = productService.getCategoryList();
        return Result.success(categoryList);
    }
}
