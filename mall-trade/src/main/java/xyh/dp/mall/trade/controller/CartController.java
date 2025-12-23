package xyh.dp.mall.trade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.annotation.RequireLogin;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.AddCartItemDTO;
import xyh.dp.mall.trade.dto.UpdateCartItemDTO;
import xyh.dp.mall.trade.service.CartService;
import xyh.dp.mall.trade.vo.CartSummaryVO;

/**
 * 购物车控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "购物车接口", description = "购物车增删改查、全选等操作")
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 添加商品到购物车
     * 
     * @param dto 添加购物车请求
     * @return 购物车项ID
     */
    @PostMapping("/add")
    @RequireLogin
    @Operation(summary = "添加商品到购物车", description = "如果商品已存在则增加数量")
    public Result<Long> addToCart(@Valid @RequestBody AddCartItemDTO dto) {
        log.info("添加购物车请求: productId={}, quantity={}", dto.getProductId(), dto.getQuantity());
        Long cartItemId = cartService.addToCart(dto);
        return Result.success(cartItemId);
    }
    
    /**
     * 查询购物车（含汇总信息）
     * 
     * @return 购物车汇总
     */
    @GetMapping("/summary")
    @RequireLogin
    @Operation(summary = "查询购物车", description = "获取购物车列表和汇总信息")
    public Result<CartSummaryVO> getCartSummary() {
        log.info("查询购物车");
        CartSummaryVO summary = cartService.getCartSummary();
        return Result.success(summary);
    }
    
    /**
     * 更新购物车数量
     * 
     * @param dto 更新请求
     * @return 操作结果
     */
    @PutMapping("/quantity")
    @RequireLogin
    @Operation(summary = "更新购物车数量", description = "修改购物车项的数量")
    public Result<Void> updateQuantity(@Valid @RequestBody UpdateCartItemDTO dto) {
        log.info("更新购物车数量: id={}, quantity={}", dto.getId(), dto.getQuantity());
        cartService.updateQuantity(dto);
        return Result.success();
    }
    
    /**
     * 切换购物车项选中状态
     * 
     * @param cartItemId 购物车项ID
     * @param selected 是否选中
     * @return 操作结果
     */
    @PutMapping("/{cartItemId}/select")
    @RequireLogin
    @Operation(summary = "切换选中状态", description = "切换购物车项的选中状态")
    public Result<Void> toggleSelected(
            @Parameter(description = "购物车项ID") @PathVariable Long cartItemId,
            @Parameter(description = "是否选中") @RequestParam Boolean selected
    ) {
        log.info("切换购物车选中状态: id={}, selected={}", cartItemId, selected);
        cartService.toggleSelected(cartItemId, selected);
        return Result.success();
    }
    
    /**
     * 全选/全不选
     * 
     * @param selected 是否全选
     * @return 操作结果
     */
    @PutMapping("/select-all")
    @RequireLogin
    @Operation(summary = "全选/全不选", description = "一键全选或全不选购物车项")
    public Result<Void> selectAll(@Parameter(description = "是否全选") @RequestParam Boolean selected) {
        log.info("购物车全选/全不选: selected={}", selected);
        cartService.selectAll(selected);
        return Result.success();
    }
    
    /**
     * 删除购物车项
     * 
     * @param cartItemId 购物车项ID
     * @return 操作结果
     */
    @DeleteMapping("/{cartItemId}")
    @RequireLogin
    @Operation(summary = "删除购物车项", description = "从购物车中移除指定商品")
    public Result<Void> removeItem(@Parameter(description = "购物车项ID") @PathVariable Long cartItemId) {
        log.info("删除购物车项: id={}", cartItemId);
        cartService.removeItem(cartItemId);
        return Result.success();
    }
    
    /**
     * 清空购物车
     * 
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    @RequireLogin
    @Operation(summary = "清空购物车", description = "清空当前用户的所有购物车项")
    public Result<Void> clearCart() {
        log.info("清空购物车");
        cartService.clearCart();
        return Result.success();
    }
    
    /**
     * 删除选中的购物车项
     * 
     * @return 操作结果
     */
    @DeleteMapping("/selected")
    @RequireLogin
    @Operation(summary = "删除选中项", description = "删除所有选中的购物车项")
    public Result<Void> removeSelectedItems() {
        log.info("删除选中的购物车项");
        cartService.removeSelectedItems();
        return Result.success();
    }
}
