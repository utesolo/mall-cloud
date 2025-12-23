package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.AddCartItemDTO;
import xyh.dp.mall.trade.dto.UpdateCartItemDTO;
import xyh.dp.mall.trade.entity.CartItem;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.CartItemMapper;
import xyh.dp.mall.trade.vo.CartItemVO;
import xyh.dp.mall.trade.vo.CartSummaryVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartItemMapper cartItemMapper;
    private final ProductFeignClient productFeignClient;
    
    /**
     * 添加商品到购物车
     * 如果商品已存在，增加数量
     * 
     * @param dto 添加购物车请求
     * @return 购物车项ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addToCart(AddCartItemDTO dto) {
        Long userId = UserContextHolder.getUserId();
        log.info("添加购物车: userId={}, productId={}, quantity={}", userId, dto.getProductId(), dto.getQuantity());
        
        // 查询商品信息
        Result<ProductDTO> productResult = productFeignClient.getProductById(dto.getProductId());
        if (!productResult.isSuccess() || productResult.getData() == null) {
            throw new BusinessException("商品不存在");
        }
        
        ProductDTO product = productResult.getData();
        
        // 检查商品状态
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }
        
        // 检查库存
        if (product.getStock() < dto.getQuantity()) {
            throw new BusinessException("库存不足");
        }
        
        // 检查是否已存在
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId)
                    .eq(CartItem::getProductId, dto.getProductId());
        CartItem existingItem = cartItemMapper.selectOne(queryWrapper);
        
        if (existingItem != null) {
            // 已存在，增加数量
            Integer newQuantity = existingItem.getQuantity() + dto.getQuantity();
            if (newQuantity > product.getStock()) {
                throw new BusinessException("库存不足");
            }
            
            existingItem.setQuantity(newQuantity);
            existingItem.setUpdateTime(LocalDateTime.now());
            cartItemMapper.updateById(existingItem);
            
            log.info("购物车数量更新: id={}, newQuantity={}", existingItem.getId(), newQuantity);
            return existingItem.getId();
            
        } else {
            // 不存在，新增
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(dto.getProductId());
            cartItem.setProductName(product.getName());
            cartItem.setProductImage(product.getMainImage());
            cartItem.setPrice(product.getPrice());
            cartItem.setQuantity(dto.getQuantity());
            cartItem.setSelected(true); // 默认选中
            cartItem.setCreateTime(LocalDateTime.now());
            cartItem.setUpdateTime(LocalDateTime.now());
            
            cartItemMapper.insert(cartItem);
            log.info("购物车新增成功: id={}", cartItem.getId());
            return cartItem.getId();
        }
    }
    
    /**
     * 查询购物车列表（含汇总信息）
     * 
     * @return 购物车汇总
     */
    public CartSummaryVO getCartSummary() {
        Long userId = UserContextHolder.getUserId();
        log.info("查询购物车: userId={}", userId);
        
        // 查询购物车项
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId)
                    .orderByDesc(CartItem::getCreateTime);
        List<CartItem> cartItems = cartItemMapper.selectList(queryWrapper);
        
        // 转换为VO并查询最新商品信息
        List<CartItemVO> itemVOList = cartItems.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 计算汇总信息
        CartSummaryVO summary = new CartSummaryVO();
        summary.setItems(itemVOList);
        
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int selectedQuantity = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        
        for (CartItemVO item : itemVOList) {
            totalQuantity += item.getQuantity();
            totalAmount = totalAmount.add(item.getSubtotal());
            
            if (item.getSelected()) {
                selectedQuantity += item.getQuantity();
                selectedAmount = selectedAmount.add(item.getSubtotal());
            }
        }
        
        summary.setTotalQuantity(totalQuantity);
        summary.setTotalAmount(totalAmount);
        summary.setSelectedQuantity(selectedQuantity);
        summary.setSelectedAmount(selectedAmount);
        
        return summary;
    }
    
    /**
     * 更新购物车数量
     * 
     * @param dto 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(UpdateCartItemDTO dto) {
        Long userId = UserContextHolder.getUserId();
        log.info("更新购物车数量: userId={}, cartItemId={}, quantity={}", userId, dto.getId(), dto.getQuantity());
        
        CartItem cartItem = cartItemMapper.selectById(dto.getId());
        if (cartItem == null) {
            throw new BusinessException("购物车项不存在");
        }
        
        // 校验用户权限
        if (!cartItem.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此购物车项");
        }
        
        // 查询商品库存
        Result<ProductDTO> productResult = productFeignClient.getProductById(cartItem.getProductId());
        if (productResult.isSuccess() && productResult.getData() != null) {
            ProductDTO product = productResult.getData();
            if (dto.getQuantity() > product.getStock()) {
                throw new BusinessException("库存不足，当前库存: " + product.getStock());
            }
        }
        
        cartItem.setQuantity(dto.getQuantity());
        cartItem.setUpdateTime(LocalDateTime.now());
        cartItemMapper.updateById(cartItem);
        
        log.info("购物车数量更新成功: id={}", dto.getId());
    }
    
    /**
     * 切换购物车项选中状态
     * 
     * @param cartItemId 购物车项ID
     * @param selected 是否选中
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleSelected(Long cartItemId, Boolean selected) {
        Long userId = UserContextHolder.getUserId();
        log.info("切换购物车选中状态: userId={}, cartItemId={}, selected={}", userId, cartItemId, selected);
        
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            throw new BusinessException("购物车项不存在");
        }
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此购物车项");
        }
        
        cartItem.setSelected(selected);
        cartItem.setUpdateTime(LocalDateTime.now());
        cartItemMapper.updateById(cartItem);
        
        log.info("购物车选中状态更新成功: id={}", cartItemId);
    }
    
    /**
     * 全选/全不选
     * 
     * @param selected 是否全选
     */
    @Transactional(rollbackFor = Exception.class)
    public void selectAll(Boolean selected) {
        Long userId = UserContextHolder.getUserId();
        log.info("购物车全选/全不选: userId={}, selected={}", userId, selected);
        
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId);
        List<CartItem> cartItems = cartItemMapper.selectList(queryWrapper);
        
        for (CartItem item : cartItems) {
            item.setSelected(selected);
            item.setUpdateTime(LocalDateTime.now());
            cartItemMapper.updateById(item);
        }
        
        log.info("购物车全选/全不选完成: count={}", cartItems.size());
    }
    
    /**
     * 删除购物车项
     * 
     * @param cartItemId 购物车项ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long cartItemId) {
        Long userId = UserContextHolder.getUserId();
        log.info("删除购物车项: userId={}, cartItemId={}", userId, cartItemId);
        
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            throw new BusinessException("购物车项不存在");
        }
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此购物车项");
        }
        
        cartItemMapper.deleteById(cartItemId);
        log.info("购物车项删除成功: id={}", cartItemId);
    }
    
    /**
     * 清空购物车
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearCart() {
        Long userId = UserContextHolder.getUserId();
        log.info("清空购物车: userId={}", userId);
        
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId);
        cartItemMapper.delete(queryWrapper);
        
        log.info("购物车清空成功");
    }
    
    /**
     * 删除选中的购物车项
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeSelectedItems() {
        Long userId = UserContextHolder.getUserId();
        log.info("删除选中的购物车项: userId={}", userId);
        
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId)
                    .eq(CartItem::getSelected, true);
        cartItemMapper.delete(queryWrapper);
        
        log.info("选中的购物车项删除成功");
    }
    
    /**
     * 将CartItem转换为CartItemVO
     * 
     * @param cartItem 购物车项实体
     * @return 购物车项VO
     */
    private CartItemVO convertToVO(CartItem cartItem) {
        CartItemVO vo = new CartItemVO();
        
        vo.setId(cartItem.getId());
        vo.setProductId(cartItem.getProductId());
        vo.setProductName(cartItem.getProductName());
        vo.setProductImage(cartItem.getProductImage());
        vo.setPrice(cartItem.getPrice());
        vo.setQuantity(cartItem.getQuantity());
        vo.setSelected(cartItem.getSelected());
        vo.setCreateTime(cartItem.getCreateTime());
        vo.setUpdateTime(cartItem.getUpdateTime());
        
        // 计算小计
        BigDecimal subtotal = cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
        vo.setSubtotal(subtotal);
        
        // 查询最新商品信息（库存、状态）
        try {
            Result<ProductDTO> productResult = productFeignClient.getProductById(cartItem.getProductId());
            if (productResult.isSuccess() && productResult.getData() != null) {
                ProductDTO product = productResult.getData();
                vo.setStock(product.getStock());
                vo.setStatus(product.getStatus());
                
                // 如果价格变动，更新购物车中的价格
                if (product.getPrice().compareTo(cartItem.getPrice()) != 0) {
                    vo.setPrice(product.getPrice());
                    vo.setSubtotal(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
                }
            }
        } catch (Exception e) {
            log.warn("查询商品信息失败: productId={}", cartItem.getProductId(), e);
            // 查询失败时使用购物车中的数据
            vo.setStock(0);
            vo.setStatus("UNKNOWN");
        }
        
        return vo;
    }
}
