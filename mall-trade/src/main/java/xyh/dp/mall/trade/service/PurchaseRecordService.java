package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.trade.entity.PurchaseRecord;
import xyh.dp.mall.trade.mapper.PurchaseRecordMapper;
import xyh.dp.mall.trade.vo.PurchaseRecordVO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购买记录服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRecordService {
    
    private final PurchaseRecordMapper purchaseRecordMapper;
    
    /**
     * 保存购买记录
     * 订单支付成功后调用
     * 
     * @param purchaseRecord 购买记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePurchaseRecord(PurchaseRecord purchaseRecord) {
        log.info("保存购买记录: userId={}, productId={}, quantity={}", 
                purchaseRecord.getUserId(), purchaseRecord.getProductId(), purchaseRecord.getQuantity());
        
        purchaseRecord.setCreateTime(LocalDateTime.now());
        purchaseRecordMapper.insert(purchaseRecord);
        
        log.info("购买记录保存成功: id={}", purchaseRecord.getId());
    }
    
    /**
     * 分页查询用户的购买记录
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 购买记录分页数据
     */
    public Page<PurchaseRecordVO> pageMyRecords(Integer pageNum, Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        log.info("查询购买记录: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);
        
        Page<PurchaseRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseRecord::getUserId, userId)
                    .orderByDesc(PurchaseRecord::getPurchaseTime);
        
        Page<PurchaseRecord> recordPage = purchaseRecordMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<PurchaseRecordVO> voPage = new Page<>(pageNum, pageSize, recordPage.getTotal());
        List<PurchaseRecordVO> voList = recordPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    /**
     * 查询用户的购买统计
     * 
     * @return 购买统计数据
     */
    public Map<String, Object> getMyStatistics() {
        Long userId = UserContextHolder.getUserId();
        log.info("查询购买统计: userId={}", userId);
        
        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseRecord::getUserId, userId);
        
        List<PurchaseRecord> records = purchaseRecordMapper.selectList(queryWrapper);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 总购买次数
        statistics.put("totalCount", records.size());
        
        // 总购买金额
        double totalAmount = records.stream()
                .mapToDouble(r -> r.getTotalAmount().doubleValue())
                .sum();
        statistics.put("totalAmount", totalAmount);
        
        // 购买的商品种类数
        long productCount = records.stream()
                .map(PurchaseRecord::getProductId)
                .distinct()
                .count();
        statistics.put("productCount", productCount);
        
        // 最近一次购买时间
        LocalDateTime lastPurchaseTime = records.stream()
                .map(PurchaseRecord::getPurchaseTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        statistics.put("lastPurchaseTime", lastPurchaseTime);
        
        // 最常购买的品种(Top 5)
        Map<String, Long> varietyCount = records.stream()
                .filter(r -> r.getVariety() != null)
                .collect(Collectors.groupingBy(
                        PurchaseRecord::getVariety,
                        Collectors.counting()
                ));
        
        List<Map.Entry<String, Long>> topVarieties = varietyCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .toList();
        
        statistics.put("topVarieties", topVarieties);
        
        return statistics;
    }
    
    /**
     * 查询用户是否购买过某商品
     * 
     * @param productId 商品ID
     * @return 是否购买过
     */
    public boolean hasPurchased(Long productId) {
        Long userId = UserContextHolder.getUserId();
        
        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseRecord::getUserId, userId)
                    .eq(PurchaseRecord::getProductId, productId);
        
        return purchaseRecordMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 查询用户购买过的商品ID列表
     * 
     * @return 商品ID列表
     */
    public List<Long> getMyPurchasedProductIds() {
        Long userId = UserContextHolder.getUserId();
        
        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseRecord::getUserId, userId)
                    .select(PurchaseRecord::getProductId);
        
        return purchaseRecordMapper.selectList(queryWrapper).stream()
                .map(PurchaseRecord::getProductId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 将PurchaseRecord转换为PurchaseRecordVO
     * 
     * @param record 购买记录实体
     * @return 购买记录VO
     */
    private PurchaseRecordVO convertToVO(PurchaseRecord record) {
        PurchaseRecordVO vo = new PurchaseRecordVO();
        
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setProductId(record.getProductId());
        vo.setProductName(record.getProductName());
        vo.setCategoryName(record.getCategoryName());
        vo.setVariety(record.getVariety());
        vo.setOrigin(record.getOrigin());
        vo.setPrice(record.getPrice());
        vo.setQuantity(record.getQuantity());
        vo.setTotalAmount(record.getTotalAmount());
        vo.setPurchaseTime(record.getPurchaseTime());
        
        return vo;
    }
}
