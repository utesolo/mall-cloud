package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.entity.PurchaseRecord;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.PurchaseRecordMapper;
import xyh.dp.mall.trade.vo.HotProductVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 热销商品服务
 * 统计一周内的热销商品
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotProductService {

    private final PurchaseRecordMapper purchaseRecordMapper;
    private final ProductFeignClient productFeignClient;

    /**
     * 查询一周内的热销商品排行（前10）
     * 
     * @return 热销商品列表
     */
    public List<HotProductVO> getWeeklyHotProducts() {
        return getWeeklyHotProducts(10);
    }

    /**
     * 查询一周内的热销商品排行
     * 
     * @param topN 返回前N个商品
     * @return 热销商品列表
     */
    public List<HotProductVO> getWeeklyHotProducts(Integer topN) {
        log.info("查询一周热销商品排行, topN={}", topN);

        // 计算一周前的时间
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        // 查询一周内的所有购买记录
        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(PurchaseRecord::getPurchaseTime, oneWeekAgo)
                .orderByDesc(PurchaseRecord::getPurchaseTime);

        List<PurchaseRecord> records = purchaseRecordMapper.selectList(queryWrapper);

        if (records.isEmpty()) {
            log.info("一周内没有购买记录");
            return Collections.emptyList();
        }

        log.info("一周内共有 {} 条购买记录", records.size());

        // 按商品ID分组统计
        Map<Long, List<PurchaseRecord>> groupedByProduct = records.stream()
                .collect(Collectors.groupingBy(PurchaseRecord::getProductId));

        // 构建热销商品统计数据
        List<HotProductVO> hotProducts = new ArrayList<>();

        for (Map.Entry<Long, List<PurchaseRecord>> entry : groupedByProduct.entrySet()) {
            Long productId = entry.getKey();
            List<PurchaseRecord> productRecords = entry.getValue();

            // 统计该商品的销量、购买人数、销售额
            int weekSales = productRecords.stream()
                    .mapToInt(PurchaseRecord::getQuantity)
                    .sum();

            long weekBuyerCount = productRecords.stream()
                    .map(PurchaseRecord::getUserId)
                    .distinct()
                    .count();

            BigDecimal weekTotalAmount = productRecords.stream()
                    .map(PurchaseRecord::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 获取第一条记录作为商品基础信息
            PurchaseRecord firstRecord = productRecords.get(0);

            HotProductVO vo = new HotProductVO();
            vo.setProductId(productId);
            vo.setProductName(firstRecord.getProductName());
            vo.setCategoryId(firstRecord.getCategoryId());
            vo.setCategoryName(firstRecord.getCategoryName());
            vo.setVariety(firstRecord.getVariety());
            vo.setOrigin(firstRecord.getOrigin());
            vo.setPrice(firstRecord.getPrice());
            vo.setSupplierId(firstRecord.getSupplierId());
            vo.setWeekSales(weekSales);
            vo.setWeekBuyerCount((int) weekBuyerCount);
            vo.setWeekTotalAmount(weekTotalAmount);

            hotProducts.add(vo);
        }

        // 按销量降序排序，取前N个
        List<HotProductVO> topProducts = hotProducts.stream()
                .sorted(Comparator.comparingInt(HotProductVO::getWeekSales).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        // 设置排名
        for (int i = 0; i < topProducts.size(); i++) {
            topProducts.get(i).setRank(i + 1);
        }

        // 补充商品的实时信息（主图、库存）
        enrichProductInfo(topProducts);

        log.info("一周热销商品排行统计完成, 共 {} 个商品", topProducts.size());
        return topProducts;
    }

    /**
     * 查询指定分类的一周热销商品
     * 
     * @param categoryId 分类ID
     * @param topN 返回前N个商品
     * @return 热销商品列表
     */
    public List<HotProductVO> getWeeklyHotProductsByCategory(Long categoryId, Integer topN) {
        log.info("查询分类热销商品, categoryId={}, topN={}", categoryId, topN);

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        LambdaQueryWrapper<PurchaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(PurchaseRecord::getPurchaseTime, oneWeekAgo)
                .eq(PurchaseRecord::getCategoryId, categoryId)
                .orderByDesc(PurchaseRecord::getPurchaseTime);

        List<PurchaseRecord> records = purchaseRecordMapper.selectList(queryWrapper);

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 按商品ID分组统计
        Map<Long, List<PurchaseRecord>> groupedByProduct = records.stream()
                .collect(Collectors.groupingBy(PurchaseRecord::getProductId));

        List<HotProductVO> hotProducts = new ArrayList<>();

        for (Map.Entry<Long, List<PurchaseRecord>> entry : groupedByProduct.entrySet()) {
            Long productId = entry.getKey();
            List<PurchaseRecord> productRecords = entry.getValue();

            int weekSales = productRecords.stream()
                    .mapToInt(PurchaseRecord::getQuantity)
                    .sum();

            long weekBuyerCount = productRecords.stream()
                    .map(PurchaseRecord::getUserId)
                    .distinct()
                    .count();

            BigDecimal weekTotalAmount = productRecords.stream()
                    .map(PurchaseRecord::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PurchaseRecord firstRecord = productRecords.get(0);

            HotProductVO vo = new HotProductVO();
            vo.setProductId(productId);
            vo.setProductName(firstRecord.getProductName());
            vo.setCategoryId(firstRecord.getCategoryId());
            vo.setCategoryName(firstRecord.getCategoryName());
            vo.setVariety(firstRecord.getVariety());
            vo.setOrigin(firstRecord.getOrigin());
            vo.setPrice(firstRecord.getPrice());
            vo.setSupplierId(firstRecord.getSupplierId());
            vo.setWeekSales(weekSales);
            vo.setWeekBuyerCount((int) weekBuyerCount);
            vo.setWeekTotalAmount(weekTotalAmount);

            hotProducts.add(vo);
        }

        List<HotProductVO> topProducts = hotProducts.stream()
                .sorted(Comparator.comparingInt(HotProductVO::getWeekSales).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        for (int i = 0; i < topProducts.size(); i++) {
            topProducts.get(i).setRank(i + 1);
        }

        enrichProductInfo(topProducts);

        return topProducts;
    }

    /**
     * 补充商品的实时信息（主图、库存）
     * 通过Feign调用商品服务获取最新数据
     * 
     * @param hotProducts 热销商品列表
     */
    private void enrichProductInfo(List<HotProductVO> hotProducts) {
        for (HotProductVO vo : hotProducts) {
            try {
                Result<ProductDTO> result = productFeignClient.getProductById(vo.getProductId());
                if (result.isSuccess() && result.getData() != null) {
                    ProductDTO product = result.getData();
                    vo.setProductImage(product.getMainImage());
                    vo.setStock(product.getStock());
                    
                    // 更新价格（可能有变动）
                    vo.setPrice(product.getPrice());
                }
            } catch (Exception e) {
                log.warn("获取商品信息失败, productId: {}", vo.getProductId(), e);
            }
        }
    }
}
