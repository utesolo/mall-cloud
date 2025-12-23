package xyh.dp.mall.search.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.search.document.ProductDocument;
import xyh.dp.mall.search.feign.ProductFeignClient;
import xyh.dp.mall.search.repository.ProductRepository;
import xyh.dp.mall.search.vo.ProductSearchVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商品数据同步服务
 * 负责将MySQL商品数据同步到Elasticsearch
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {
    
    private final ProductRepository productRepository;
    private final ProductFeignClient productFeignClient;
    
    /**
     * 全量同步商品数据
     * 从MySQL分页查询所有商品，同步到ES
     * 
     * @return 同步的商品数量
     */
    public int syncAll() {
        log.info("开始全量同步商品数据到ES...");
        
        int totalSynced = 0;
        int pageNum = 1;
        int pageSize = 100;
        boolean hasMore = true;
        
        // 清空现有数据
        productRepository.deleteAll();
        log.info("已清空ES中的商品数据");
        
        while (hasMore) {
            try {
                // 从商品服务查询数据
                Result<List<ProductSearchVO>> result = productFeignClient.pageQuery(pageNum, pageSize);
                
                if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                    hasMore = false;
                    continue;
                }
                
                List<ProductSearchVO> products = result.getData();
                log.info("查询到第{}页数据，共{}条", pageNum, products.size());
                
                // 转换并保存到ES
                List<ProductDocument> documents = products.stream()
                        .map(this::convertToDocument)
                        .toList();
                
                productRepository.saveAll(documents);
                totalSynced += documents.size();
                
                log.info("第{}页同步完成，已同步{}条", pageNum, totalSynced);
                
                // 如果返回的数据少于pageSize，说明是最后一页
                if (products.size() < pageSize) {
                    hasMore = false;
                }
                
                pageNum++;
                
            } catch (Exception e) {
                log.error("全量同步失败，页码: {}", pageNum, e);
                throw new BusinessException("全量同步失败: " + e.getMessage());
            }
        }
        
        log.info("全量同步完成，共同步{}条商品数据", totalSynced);
        return totalSynced;
    }
    
    /**
     * 增量同步单个商品
     * 商品创建或更新时调用
     * 
     * @param productId 商品ID
     * @return 是否成功
     */
    public boolean syncOne(Long productId) {
        log.info("增量同步商品: {}", productId);
        
        try {
            // 从商品服务查询商品详情
            Result<ProductSearchVO> result = productFeignClient.getById(productId);
            
            if (!result.isSuccess() || result.getData() == null) {
                log.error("查询商品失败: {}", productId);
                return false;
            }
            
            ProductSearchVO product = result.getData();
            
            // 转换并保存到ES
            ProductDocument document = convertToDocument(product);
            productRepository.save(document);
            
            log.info("商品同步成功: id={}, name={}", productId, product.getName());
            return true;
            
        } catch (Exception e) {
            log.error("商品同步失败: {}", productId, e);
            return false;
        }
    }
    
    /**
     * 删除商品
     * 商品删除或下架时调用
     * 
     * @param productId 商品ID
     * @return 是否成功
     */
    public boolean deleteOne(Long productId) {
        log.info("从ES删除商品: {}", productId);
        
        try {
            productRepository.deleteById(productId);
            log.info("商品删除成功: {}", productId);
            return true;
            
        } catch (Exception e) {
            log.error("商品删除失败: {}", productId, e);
            return false;
        }
    }
    
    /**
     * 批量同步商品
     * 
     * @param productIds 商品ID列表
     * @return 成功同步的数量
     */
    public int syncBatch(List<Long> productIds) {
        log.info("批量同步商品，数量: {}", productIds.size());
        
        int successCount = 0;
        for (Long productId : productIds) {
            if (syncOne(productId)) {
                successCount++;
            }
        }
        
        log.info("批量同步完成，成功: {}/{}", successCount, productIds.size());
        return successCount;
    }
    
    /**
     * 检查商品是否已存在于ES
     * 
     * @param productId 商品ID
     * @return 是否存在
     */
    public boolean exists(Long productId) {
        return productRepository.existsById(productId);
    }
    
    /**
     * 获取ES中的商品总数
     * 
     * @return 商品总数
     */
    public long count() {
        return productRepository.count();
    }
    
    /**
     * 将ProductSearchVO转换为ProductDocument
     * 
     * @param vo 商品VO
     * @return ES文档
     */
    private ProductDocument convertToDocument(ProductSearchVO vo) {
        ProductDocument doc = new ProductDocument();
        
        // 基础信息
        doc.setId(vo.getId());
        doc.setName(vo.getName());
        doc.setCategoryId(vo.getCategoryId());
        doc.setCategoryName(vo.getCategoryName());
        doc.setMainImage(vo.getMainImage());
        doc.setDescription(vo.getDescription());
        doc.setSpecification(vo.getSpecification());
        doc.setPrice(vo.getPrice());
        doc.setStock(vo.getStock());
        doc.setSales(vo.getSales());
        doc.setSupplierId(vo.getSupplierId());
        doc.setStatus(vo.getStatus());
        
        // 种子特有属性
        doc.setOrigin(vo.getOrigin());
        doc.setVariety(vo.getVariety());
        doc.setDifficulty(vo.getDifficulty());
        doc.setGrowthCycle(vo.getGrowthCycle());
        doc.setGerminationRate(vo.getGerminationRate());
        doc.setPurity(vo.getPurity());
        
        // 种植环境要求
        doc.setMinTemperature(vo.getMinTemperature());
        doc.setMaxTemperature(vo.getMaxTemperature());
        doc.setMinHumidity(vo.getMinHumidity());
        doc.setMaxHumidity(vo.getMaxHumidity());
        doc.setMinPh(vo.getMinPh());
        doc.setMaxPh(vo.getMaxPh());
        doc.setLightRequirement(vo.getLightRequirement());
        
        // 列表字段
        doc.setImages(vo.getImages() != null ? vo.getImages() : new ArrayList<>());
        doc.setRegions(vo.getRegions() != null ? vo.getRegions() : new ArrayList<>());
        doc.setPlantingSeasons(vo.getPlantingSeasons() != null ? vo.getPlantingSeasons() : new ArrayList<>());
        
        return doc;
    }
}
