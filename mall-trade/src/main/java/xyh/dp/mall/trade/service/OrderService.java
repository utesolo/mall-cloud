package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreateOrderDTO;
import xyh.dp.mall.trade.entity.Order;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.OrderMapper;
import xyh.dp.mall.trade.vo.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 订单服务
 * 支持OpenFeign调用商品服务、多线程并行处理、事务一致性保证
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    // TODO 折扣功能，部分商品添加特价等活动折扣

    private final OrderMapper orderMapper;
    private final ProductFeignClient productFeignClient;
    
    @Qualifier("orderExecutor")
    private final Executor orderExecutor;

    /**
     * 创建订单
     * 使用TCC模式保证分布式事务一致性：
     * 1. Try: 查询商品信息 + 预扣库存
     * 2. Confirm: 创建订单 + 增加销量
     * 3. Cancel: 回滚库存（异常时）
     * 
     * @param createOrderDTO 创建订单请求
     * @return 订单信息
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO createOrderDTO) {
        Long productId = createOrderDTO.getProductId();
        Integer quantity = createOrderDTO.getQuantity();
        
        // 1. 并行执行：查询商品信息 + 预扣库存
        ProductDTO product = executePreOrderTasks(productId, quantity);
        
        // 2. 生成订单号
        String orderNo = generateOrderNo();
        
        // 3. 计算订单总金额
        BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(quantity));
        
        // 4. 创建订单
        Order order = buildOrder(createOrderDTO, orderNo, product, totalAmount);
        orderMapper.insert(order);
        log.info("创建订单成功, orderNo: {}, userId: {}, productId: {}", 
                orderNo, createOrderDTO.getUserId(), productId);
        
        // 5. 事务提交后异步增加销量（保证一致性）
        registerAfterCommitTask(productId, quantity);
        
        return convertToVO(order);
    }

    /**
     * 执行订单前置任务（并行执行）
     * 并行查询商品信息和预扣库存
     * 
     * @param productId 商品ID
     * @param quantity 购买数量
     * @return 商品信息
     * @throws BusinessException 商品不存在或库存不足
     */
    private ProductDTO executePreOrderTasks(Long productId, Integer quantity) {
        // 并行执行查询商品和扣减库存
        CompletableFuture<ProductDTO> productFuture = CompletableFuture.supplyAsync(
                () -> getProductInfo(productId), orderExecutor);
        
        CompletableFuture<Void> deductFuture = CompletableFuture.runAsync(
                () -> deductStock(productId, quantity), orderExecutor);
        
        try {
            // 等待所有任务完成
            CompletableFuture.allOf(productFuture, deductFuture).join();
            return productFuture.get();
        } catch (Exception e) {
            log.error("订单前置任务执行失败, productId: {}", productId, e);
            // 如果库存已扣，需要回滚
            tryRestoreStock(productId, quantity);
            throw new BusinessException("创建订单失败: " + getRootCauseMessage(e));
        }
    }

    /**
     * 获取商品信息
     * 
     * @param productId 商品ID
     * @return 商品信息
     * @throws BusinessException 商品不存在或已下架
     */
    private ProductDTO getProductInfo(Long productId) {
        log.debug("查询商品信息, productId: {}", productId);
        Result<ProductDTO> result = productFeignClient.getProductById(productId);
        
        if (result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        
        ProductDTO product = result.getData();
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }
        
        return product;
    }

    /**
     * 扣减库存
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @throws BusinessException 库存不足
     */
    private void deductStock(Long productId, Integer quantity) {
        log.debug("扣减库存, productId: {}, quantity: {}", productId, quantity);
        Result<Boolean> result = productFeignClient.deductStock(productId, quantity);
        
        if (result.getCode() != 200 || !Boolean.TRUE.equals(result.getData())) {
            throw new BusinessException("库存不足");
        }
    }

    /**
     * 尝试恢复库存（用于补偿）
     * 
     * @param productId 商品ID
     * @param quantity 恢复数量
     */
    private void tryRestoreStock(Long productId, Integer quantity) {
        try {
            log.info("尝试恢复库存, productId: {}, quantity: {}", productId, quantity);
            productFeignClient.restoreStock(productId, quantity);
        } catch (Exception e) {
            log.error("恢复库存失败, 需要人工处理, productId: {}, quantity: {}", 
                    productId, quantity, e);
        }
    }

    /**
     * 注册事务提交后的异步任务
     * 确保订单提交成功后才增加销量
     * 
     * @param productId 商品ID
     * @param quantity 数量
     */
    private void registerAfterCommitTask(Long productId, Integer quantity) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                productFeignClient.increaseSales(productId, quantity);
                                log.debug("增加商品销量成功, productId: {}", productId);
                            } catch (Exception e) {
                                log.error("增加商品销量失败, productId: {}", productId, e);
                            }
                        }, orderExecutor);
                    }
                });
    }

    /**
     * 构建订单实体
     * 
     * @param createOrderDTO 创建订单请求
     * @param orderNo 订单号
     * @param product 商品信息
     * @param totalAmount 订单总额
     * @return 订单实体
     */
    private Order buildOrder(CreateOrderDTO createOrderDTO, String orderNo, 
                              ProductDTO product, BigDecimal totalAmount) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(createOrderDTO.getUserId());
        order.setProductId(createOrderDTO.getProductId());
        order.setProductName(product.getName());
        order.setProductImage(product.getMainImage());
        order.setPrice(product.getPrice());
        order.setQuantity(createOrderDTO.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setReceiverName(createOrderDTO.getReceiverName());
        order.setReceiverPhone(createOrderDTO.getReceiverPhone());
        order.setReceiverAddress(createOrderDTO.getReceiverAddress());
        order.setRemark(createOrderDTO.getRemark());
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
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
    public Page<OrderVO> pageQuery(Long userId, Integer pageNum, Integer pageSize, String status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.eq(Order::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        Page<Order> orderPage = orderMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<OrderVO> voPage = new Page<>(pageNum, pageSize, orderPage.getTotal());
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单信息
     * @throws BusinessException 订单不存在
     */
    public OrderVO getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(queryWrapper);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        return convertToVO(order);
    }

    /**
     * 取消订单
     * 取消订单时需要恢复库存
     * 
     * @param orderNo 订单号
     * @param userId 用户ID
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        queryWrapper.eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(queryWrapper);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只能取消待支付订单");
        }
        
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 恢复库存
        tryRestoreStock(order.getProductId(), order.getQuantity());
        
        log.info("取消订单成功, orderNo: {}", orderNo);
    }

    /**
     * 生成订单号
     * 
     * @return 订单号
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return "ORD" + date + String.format("%04d", random);
    }

    /**
     * 获取异常根因消息
     * 
     * @param e 异常
     * @return 根因消息
     */
    private String getRootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    /**
     * 将Order转换为OrderVO
     * 
     * @param order 订单实体
     * @return 订单VO
     */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}
