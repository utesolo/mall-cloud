package xyh.dp.mall.product.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.product.entity.Category;
import xyh.dp.mall.product.entity.Product;
import xyh.dp.mall.product.mapper.CategoryMapper;
import xyh.dp.mall.product.mapper.ProductMapper;
import xyh.dp.mall.product.vo.ProductVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    // TODO 添加多线程，在多人请求时保护数据同步
    // TODO 通过售卖的商品数量来进行销售排序
    // TODO 最近买过
    // TODO 最近多人买

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

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
    public Page<ProductVO> pageQuery(Integer pageNum, Integer pageSize, Long categoryId, 
                                      String keyword, BigDecimal minPrice, BigDecimal maxPrice, String region) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询上架商品
        queryWrapper.eq(Product::getStatus, "ON_SALE");
        
        // 分类筛选
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Product::getName, keyword)
                    .or().like(Product::getDescription, keyword);
        }
        
        // 价格区间筛选
        if (minPrice != null) {
            queryWrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le(Product::getPrice, maxPrice);
        }
        
        // 区域筛选
        if (StringUtils.hasText(region)) {
            queryWrapper.like(Product::getRegions, region);
        }
        
        // 按销量降序排序
        queryWrapper.orderByDesc(Product::getSales);
        
        Page<Product> productPage = productMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<ProductVO> voPage = new Page<>(pageNum, pageSize, productPage.getTotal());
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    /**
     * 根据ID查询商品详情
     * 
     * @param id 商品ID
     * @return 商品详情
     * @throws BusinessException 商品不存在
     */
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return convertToVO(product);
    }

    /**
     * 查询分类列表
     * 
     * @return 分类列表
     */
    public List<Category> getCategoryList() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, "NORMAL");
        queryWrapper.orderByAsc(Category::getSort);
        return categoryMapper.selectList(queryWrapper);
    }

    /**
     * 将Product转换为ProductVO
     * 
     * @param product 商品实体
     * @return 商品VO
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setCategoryId(product.getCategoryId());
        vo.setMainImage(product.getMainImage());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setDifficulty(product.getDifficulty());
        vo.setGrowthCycle(product.getGrowthCycle());
        vo.setStatus(product.getStatus());
        
        // JSON字段解析
        if (StringUtils.hasText(product.getImages())) {
            vo.setImages(JSON.parseArray(product.getImages(), String.class));
        } else {
            vo.setImages(new ArrayList<>());
        }
        
        if (StringUtils.hasText(product.getRegions())) {
            vo.setRegions(JSON.parseArray(product.getRegions(), String.class));
        } else {
            vo.setRegions(new ArrayList<>());
        }
        
        // 查询分类名称
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        
        return vo;
    }
}
