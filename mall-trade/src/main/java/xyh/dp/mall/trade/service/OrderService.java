package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.trade.dto.CreateOrderDTO;
import xyh.dp.mall.trade.entity.Order;
import xyh.dp.mall.trade.mapper.OrderMapper;
import xyh.dp.mall.trade.vo.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    /**
     * 创建订单
     * 
     * @param createOrderDTO 创建订单请求
     * @return 订单信息
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO createOrderDTO) {
        // 生成订单号
        String orderNo = generateOrderNo();
        
        // 模拟查询商品信息(实际应该调用商品服务)
        BigDecimal price = new BigDecimal("99.99");
        String productName = "优质种子-示例商品";
        String productImage = "http://example.com/product.jpg";
        
        // 计算订单总金额
        BigDecimal totalAmount = price.multiply(new BigDecimal(createOrderDTO.getQuantity()));
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(createOrderDTO.getUserId());
        order.setProductId(createOrderDTO.getProductId());
        order.setProductName(productName);
        order.setProductImage(productImage);
        order.setPrice(price);
        order.setQuantity(createOrderDTO.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setReceiverName(createOrderDTO.getReceiverName());
        order.setReceiverPhone(createOrderDTO.getReceiverPhone());
        order.setReceiverAddress(createOrderDTO.getReceiverAddress());
        order.setRemark(createOrderDTO.getRemark());
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        orderMapper.insert(order);
        log.info("创建订单成功, orderNo: {}, userId: {}", orderNo, createOrderDTO.getUserId());
        
        return convertToVO(order);
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
