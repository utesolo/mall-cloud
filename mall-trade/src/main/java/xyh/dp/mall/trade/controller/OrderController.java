package xyh.dp.mall.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreateOrderDTO;
import xyh.dp.mall.trade.service.OrderService;
import xyh.dp.mall.trade.vo.OrderVO;

/**
 * 订单控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单接口", description = "订单创建、查询、取消等接口")
public class OrderController {
    // TODO 通过微信小程序来实现支付，支付在小程序完成，返回给后端一个结果

    private final OrderService orderService;

    /**
     * 创建订单
     * 
     * @param createOrderDTO 创建订单请求
     * @return 订单信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单", description = "提交订单信息创建新订单")
    public Result<OrderVO> createOrder(@RequestBody CreateOrderDTO createOrderDTO) {
        log.info("创建订单请求: userId={}, productId={}", createOrderDTO.getUserId(), createOrderDTO.getProductId());
        OrderVO orderVO = orderService.createOrder(createOrderDTO);
        return Result.success(orderVO, "订单创建成功");
    }

    /**
     * 分页查询订单列表
     * 
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @return 订单分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询订单", description = "查询用户订单列表")
    public Result<Page<OrderVO>> page(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status
    ) {
        Page<OrderVO> page = orderService.pageQuery(userId, pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 查询订单详情
     * 
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单详情", description = "根据订单号查询订单详细信息")
    public Result<OrderVO> getByOrderNo(@Parameter(description = "订单号") @PathVariable String orderNo) {
        OrderVO orderVO = orderService.getByOrderNo(orderNo);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * 
     * @param orderNo 订单号
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单", description = "取消待支付订单")
    public Result<Void> cancelOrder(
            @Parameter(description = "订单号") @PathVariable String orderNo,
            @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        log.info("取消订单请求: orderNo={}, userId={}", orderNo, userId);
        orderService.cancelOrder(orderNo, userId);
        return Result.success(null, "订单取消成功");
    }
}
