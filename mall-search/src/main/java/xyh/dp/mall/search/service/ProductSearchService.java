package xyh.dp.mall.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyh.dp.mall.search.document.ProductDocument;
import xyh.dp.mall.search.dto.ProductSearchDTO;
import xyh.dp.mall.search.repository.ProductRepository;
import xyh.dp.mall.search.vo.ProductSearchVO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 综合搜索商品
     * 支持多条件组合搜索、全文检索、排序等功能
     * 
     * @param searchDTO 搜索条件
     * @return 搜索结果列表
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductSearchVO> searchProducts(ProductSearchDTO searchDTO) {
        log.info("商品搜索请求: {}", searchDTO);

        // 构建分页参数
        int pageNum = searchDTO.getPageNum() != null ? searchDTO.getPageNum() : 1;
        int pageSize = searchDTO.getPageSize() != null ? searchDTO.getPageSize() : 20;
        
        // 构建排序
        Sort sort;
        String sortField = searchDTO.getSortField();
        if (StringUtils.hasText(sortField)) {
            Sort.Direction direction = "asc".equalsIgnoreCase(searchDTO.getSortOrder()) 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, sortField);
        } else {
            // 默认按销量降序排序
            sort = Sort.by(Sort.Direction.DESC, "sales");
        }
        
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        
        // 执行搜索 - 使用Repository的简单查询
        org.springframework.data.domain.Page<ProductDocument> page;
        
        // 根据关键词搜索
        if (StringUtils.hasText(searchDTO.getKeyword())) {
            page = productRepository.findByNameLike(searchDTO.getKeyword(), pageable);
        } 
        // 根据品种搜索
        else if (StringUtils.hasText(searchDTO.getVariety())) {
            page = productRepository.findByVarietyLike(searchDTO.getVariety(), pageable);
        }
        // 根据产地搜索
        else if (StringUtils.hasText(searchDTO.getOrigin())) {
            page = productRepository.findByOriginLike(searchDTO.getOrigin(), pageable);
        }
        // 根据分类搜索
        else if (searchDTO.getCategoryId() != null) {
            page = productRepository.findByCategoryId(searchDTO.getCategoryId(), pageable);
        }
        // 根据区域搜索
        else if (StringUtils.hasText(searchDTO.getRegion())) {
            page = productRepository.findByRegionsContaining(searchDTO.getRegion(), pageable);
        }
        // 根据季节搜索
        else if (StringUtils.hasText(searchDTO.getSeason())) {
            page = productRepository.findByPlantingSeasonsContaining(searchDTO.getSeason(), pageable);
        }
        // 根据难度搜索
        else if (StringUtils.hasText(searchDTO.getDifficulty())) {
            page = productRepository.findByDifficulty(searchDTO.getDifficulty(), pageable);
        }
        // 默认查询所有上架商品
        else {
            page = productRepository.findByStatus("ON_SALE", pageable);
        }

        // 转换结果
        List<ProductSearchVO> voList = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductSearchVO> result =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize, page.getTotalElements());
        result.setRecords(voList);

        log.info("搜索完成, 共找到 {} 条记录", page.getTotalElements());

        return result;
    }

    /**
     * 根据ID查询商品
     * 
     * @param id 商品ID
     * @return 商品信息
     */
    public ProductSearchVO getById(Long id) {
        log.info("根据ID查询商品: id={}", id);
        return productRepository.findById(id)
                .map(this::convertToVO)
                .orElse(null);
    }

    /**
     * 热门商品推荐
     * 根据销量排序返回前N个商品
     * 
     * @param limit 返回数量
     * @return 商品列表
     */
    public List<ProductSearchVO> getHotProducts(Integer limit) {
        log.info("获取热门商品, limit={}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sales"));
        org.springframework.data.domain.Page<ProductDocument> page = productRepository.findByStatus("ON_SALE", pageable);

        return page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索建议/自动补全
     * 
     * @param keyword 关键词
     * @param limit 返回数量
     * @return 商品列表
     */
    public List<ProductSearchVO> getSuggestions(String keyword, Integer limit) {
        log.info("获取搜索建议, keyword={}, limit={}", keyword, limit);

        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(0, limit);
        org.springframework.data.domain.Page<ProductDocument> page = productRepository.findByNameLike(keyword, pageable);

        return page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将ProductDocument转换为ProductSearchVO
     * 
     * @param document ES文档
     * @return VO对象
     */
    private ProductSearchVO convertToVO(ProductDocument document) {
        ProductSearchVO vo = new ProductSearchVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }
}
