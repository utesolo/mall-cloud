package xyh.dp.mall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.annotation.RequireLogin;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.product.dto.ProductCreateDTO;
import xyh.dp.mall.product.dto.ProductUpdateDTO;
import xyh.dp.mall.product.dto.StockUpdateDTO;
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
@Tag(name = "商品接口", description = "商品查询、分类查询、商家管理等接口")
public class ProductController {

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

    // ==================== 商家商品管理接口 ====================

    /**
     * 商家新增商品
     *
     * @param dto 商品创建请求
     * @return 创建的商品ID
     */
    @PostMapping("/manage/create")
    @RequireLogin
    @Operation(summary = "新增商品", description = "商家新增商品，需要登录")
    public Result<Long> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        log.info("商家新增商品请求: {}", dto.getName());
        Long productId = productService.createProduct(dto);
        return Result.success(productId);
    }

    /**
     * 商家更新商品信息
     *
     * @param dto 商品更新请求
     * @return 操作结果
     */
    @PutMapping("/manage/update")
    @RequireLogin
    @Operation(summary = "更新商品", description = "商家更新商品信息，需要登录")
    public Result<Void> updateProduct(@Valid @RequestBody ProductUpdateDTO dto) {
        log.info("商家更新商品请求: id={}", dto.getId());
        productService.updateProduct(dto);
        return Result.success();
    }

    /**
     * 商家删除商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/manage/{id}")
    @RequireLogin
    @Operation(summary = "删除商品", description = "商家删除商品（逻辑删除），需要登录")
    public Result<Void> deleteProduct(@Parameter(description = "商品ID") @PathVariable Long id) {
        log.info("商家删除商品请求: id={}", id);
        productService.deleteProduct(id);
        return Result.success();
    }

    /**
     * 商家上架商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @PutMapping("/manage/{id}/on-sale")
    @RequireLogin
    @Operation(summary = "上架商品", description = "商家上架商品，需要登录")
    public Result<Void> onSaleProduct(@Parameter(description = "商品ID") @PathVariable Long id) {
        log.info("商家上架商品请求: id={}", id);
        productService.onSaleProduct(id);
        return Result.success();
    }

    /**
     * 商家下架商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @PutMapping("/manage/{id}/off-sale")
    @RequireLogin
    @Operation(summary = "下架商品", description = "商家下架商品，需要登录")
    public Result<Void> offSaleProduct(@Parameter(description = "商品ID") @PathVariable Long id) {
        log.info("商家下架商品请求: id={}", id);
        productService.offSaleProduct(id);
        return Result.success();
    }

    /**
     * 商家调整库存
     *
     * @param dto 库存更新请求
     * @return 操作结果
     */
    @PutMapping("/manage/stock")
    @RequireLogin
    @Operation(summary = "调整库存", description = "商家调整商品库存，需要登录")
    public Result<Void> updateStock(@Valid @RequestBody StockUpdateDTO dto) {
        log.info("商家调整库存请求: productId={}, type={}, quantity={}",
                dto.getProductId(), dto.getOperationType(), dto.getQuantity());
        productService.updateStock(dto);
        return Result.success();
    }

    /**
     * 查询商家自己的商品列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   商品状态
     * @return 商品分页数据
     */
    @GetMapping("/manage/my-products")
    @RequireLogin
    @Operation(summary = "查询我的商品", description = "商家查询自己的商品列表，需要登录")
    public Result<Page<ProductVO>> getMyProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "商品状态: ON_SALE-上架, OFF_SALE-下架") @RequestParam(required = false) String status
    ) {
        Page<ProductVO> page = productService.pageMyProducts(pageNum, pageSize, status);
        return Result.success(page);
    }

    // ==================== 内部接口（供Feign调用） ====================

    /**
     * 搜索匹配候选商品
     * 内部接口，供异步匹配服务通过Feign调用
     *
     * @param variety 品种关键词
     * @param region 区域关键词
     * @param limit 返回数量限制
     * @return 商品列表
     */
    @GetMapping("/search/match")
    @Operation(summary = "搜索匹配候选商品", description = "内部接口，供异步匹配服务调用")
    public Result<List<ProductVO>> searchForMatch(
            @Parameter(description = "品种关键词") @RequestParam(required = false) String variety,
            @Parameter(description = "区域关键词") @RequestParam(required = false) String region,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("搜索匹配候选商品: variety={}, region={}, limit={}", variety, region, limit);
        List<ProductVO> products = productService.searchForMatch(variety, region, limit);
        return Result.success(products);
    }

    /**
     * 扣减商品库存
     * 内部接口，供订单服务通过Feign调用
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 操作结果
     */
    @PostMapping("/stock/deduct")
    @Operation(summary = "扣减库存", description = "内部接口，供订单服务调用")
    public Result<Boolean> deductStock(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "扣减数量") @RequestParam Integer quantity
    ) {
        log.info("扣减库存请求: productId={}, quantity={}", productId, quantity);
        boolean success = productService.deductStock(productId, quantity);
        return Result.success(success);
    }

    /**
     * 恢复商品库存
     * 内部接口，用于订单取消或失败时回滚
     * 
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 操作结果
     */
    @PostMapping("/stock/restore")
    @Operation(summary = "恢复库存", description = "内部接口，用于订单取消时回滚库存")
    public Result<Boolean> restoreStock(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "恢复数量") @RequestParam Integer quantity
    ) {
        log.info("恢复库存请求: productId={}, quantity={}", productId, quantity);
        boolean success = productService.restoreStock(productId, quantity);
        return Result.success(success);
    }

    /**
     * 增加商品销量
     * 内部接口，订单完成后调用
     * 
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 操作结果
     */
    @PostMapping("/sales/increase")
    @Operation(summary = "增加销量", description = "内部接口，订单完成后更新销量")
    public Result<Boolean> increaseSales(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "增加数量") @RequestParam Integer quantity
    ) {
        log.info("增加销量请求: productId={}, quantity={}", productId, quantity);
        boolean success = productService.increaseSales(productId, quantity);
        return Result.success(success);
    }
}
