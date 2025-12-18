package xyh.dp.mall.product.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.product.dto.ProductCreateDTO;
import xyh.dp.mall.product.dto.ProductUpdateDTO;
import xyh.dp.mall.product.dto.StockUpdateDTO;
import xyh.dp.mall.product.entity.Category;
import xyh.dp.mall.product.entity.Product;
import xyh.dp.mall.product.mapper.CategoryMapper;
import xyh.dp.mall.product.mapper.ProductMapper;
import xyh.dp.mall.product.vo.ProductVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // ==================== 商家商品管理接口 ====================

    /**
     * 商家新增商品
     *
     * @param dto 商品创建请求
     * @return 创建的商品ID
     * @throws BusinessException 分类不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductCreateDTO dto) {
        log.info("商家新增商品: {}", dto.getName());

        // 校验分类是否存在
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }

        // 获取当前登录用户作为供应商
        Long supplierId = UserContextHolder.getUserId();

        // 构建商品实体
        Product product = buildProductFromCreateDTO(dto, supplierId);

        productMapper.insert(product);
        log.info("商品创建成功, id: {}, name: {}", product.getId(), product.getName());
        return product.getId();
    }

    /**
     * 商家更新商品信息
     *
     * @param dto 商品更新请求
     * @throws BusinessException 商品不存在或无权操作时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateDTO dto) {
        log.info("商家更新商品: id={}", dto.getId());

        Product product = productMapper.selectById(dto.getId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 校验商家权限
        Long currentUserId = UserContextHolder.getUserId();
        if (!product.getSupplierId().equals(currentUserId)) {
            throw new BusinessException("无权操作此商品");
        }

        // 如果更新分类，校验分类是否存在
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BusinessException("商品分类不存在");
            }
        }

        // 更新商品信息
        updateProductFromDTO(product, dto);
        product.setUpdateTime(LocalDateTime.now());

        productMapper.updateById(product);
        log.info("商品更新成功, id: {}", product.getId());
    }

    /**
     * 商家删除商品（逻辑删除，实际是下架）
     *
     * @param productId 商品ID
     * @throws BusinessException 商品不存在或无权操作时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        log.info("商家删除商品: id={}", productId);

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 校验商家权限
        Long currentUserId = UserContextHolder.getUserId();
        if (!product.getSupplierId().equals(currentUserId)) {
            throw new BusinessException("无权操作此商品");
        }

        // 逻辑删除：设置状态为下架
        product.setStatus("DELETED");
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        log.info("商品删除成功, id: {}", productId);
    }

    /**
     * 商家上架商品
     *
     * @param productId 商品ID
     * @throws BusinessException 商品不存在或无权操作时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void onSaleProduct(Long productId) {
        log.info("商家上架商品: id={}", productId);
        updateProductStatus(productId, "ON_SALE");
    }

    /**
     * 商家下架商品
     *
     * @param productId 商品ID
     * @throws BusinessException 商品不存在或无权操作时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void offSaleProduct(Long productId) {
        log.info("商家下架商品: id={}", productId);
        updateProductStatus(productId, "OFF_SALE");
    }

    /**
     * 商家调整库存
     *
     * @param dto 库存更新请求
     * @throws BusinessException 商品不存在、无权操作或库存不足时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(StockUpdateDTO dto) {
        log.info("商家调整库存: productId={}, operationType={}, quantity={}",
                dto.getProductId(), dto.getOperationType(), dto.getQuantity());

        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 校验商家权限
        Long currentUserId = UserContextHolder.getUserId();
        if (!product.getSupplierId().equals(currentUserId)) {
            throw new BusinessException("无权操作此商品");
        }

        Integer newStock = calculateNewStock(product.getStock(), dto);
        product.setStock(newStock);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        log.info("库存调整成功, productId: {}, oldStock: {}, newStock: {}",
                dto.getProductId(), product.getStock(), newStock);
    }

    /**
     * 查询商家自己的商品列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   商品状态（可选）
     * @return 商品分页数据
     */
    public Page<ProductVO> pageMyProducts(Integer pageNum, Integer pageSize, String status) {
        Long supplierId = UserContextHolder.getUserId();
        log.info("查询商家商品列表: supplierId={}, status={}", supplierId, status);

        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询当前商家的商品
        queryWrapper.eq(Product::getSupplierId, supplierId);

        // 排除已删除商品
        queryWrapper.ne(Product::getStatus, "DELETED");

        // 状态筛选
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Product::getStatus, status);
        }

        // 按更新时间降序
        queryWrapper.orderByDesc(Product::getUpdateTime);

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
     * 更新商品状态
     *
     * @param productId 商品ID
     * @param status    目标状态
     */
    private void updateProductStatus(Long productId, String status) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        Long currentUserId = UserContextHolder.getUserId();
        if (!product.getSupplierId().equals(currentUserId)) {
            throw new BusinessException("无权操作此商品");
        }

        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        log.info("商品状态更新成功, id: {}, status: {}", productId, status);
    }

    /**
     * 计算新库存数量
     *
     * @param currentStock 当前库存
     * @param dto          库存操作请求
     * @return 新库存数量
     */
    private Integer calculateNewStock(Integer currentStock, StockUpdateDTO dto) {
        return switch (dto.getOperationType()) {
            case "SET" -> dto.getQuantity();
            case "ADD" -> currentStock + dto.getQuantity();
            case "SUBTRACT" -> {
                int newStock = currentStock - dto.getQuantity();
                if (newStock < 0) {
                    throw new BusinessException("库存不足，当前库存: " + currentStock);
                }
                yield newStock;
            }
            default -> throw new BusinessException("无效的操作类型: " + dto.getOperationType());
        };
    }

    /**
     * 从创建DTO构建商品实体
     *
     * @param dto        创建DTO
     * @param supplierId 供应商ID
     * @return 商品实体
     */
    private Product buildProductFromCreateDTO(ProductCreateDTO dto, Long supplierId) {
        Product product = new Product();

        // 基础信息
        product.setName(dto.getName());
        product.setCategoryId(dto.getCategoryId());
        product.setMainImage(dto.getMainImage());
        product.setDescription(dto.getDescription());
        product.setSpecification(dto.getSpecification());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setSales(0);
        product.setSupplierId(supplierId);
        product.setStatus("OFF_SALE"); // 新商品默认下架状态

        // 种子特有属性
        product.setOrigin(dto.getOrigin());
        product.setVariety(dto.getVariety());
        product.setDifficulty(dto.getDifficulty());
        product.setGrowthCycle(dto.getGrowthCycle());
        product.setGerminationRate(dto.getGerminationRate());
        product.setPurity(dto.getPurity());
        product.setShelfLife(dto.getShelfLife());
        product.setProductionDate(dto.getProductionDate());

        // 种植环境
        product.setMinTemperature(dto.getMinTemperature());
        product.setMaxTemperature(dto.getMaxTemperature());
        product.setMinHumidity(dto.getMinHumidity());
        product.setMaxHumidity(dto.getMaxHumidity());
        product.setMinPh(dto.getMinPh());
        product.setMaxPh(dto.getMaxPh());
        product.setLightRequirement(dto.getLightRequirement());

        // JSON字段
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            product.setImages(JSON.toJSONString(dto.getImages()));
        }
        if (dto.getRegions() != null && !dto.getRegions().isEmpty()) {
            product.setRegions(JSON.toJSONString(dto.getRegions()));
        }
        if (dto.getPlantingSeasons() != null && !dto.getPlantingSeasons().isEmpty()) {
            product.setPlantingSeasons(JSON.toJSONString(dto.getPlantingSeasons()));
        }

        // 质量溯源
        product.setTraceCode(dto.getTraceCode());
        product.setBatchNumber(dto.getBatchNumber());
        product.setInspectionReportUrl(dto.getInspectionReportUrl());

        // 时间戳
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        return product;
    }

    /**
     * 从更新DTO更新商品实体
     *
     * @param product 商品实体
     * @param dto     更新DTO
     */
    private void updateProductFromDTO(Product product, ProductUpdateDTO dto) {
        // 基础信息（非空才更新）
        if (StringUtils.hasText(dto.getName())) {
            product.setName(dto.getName());
        }
        if (dto.getCategoryId() != null) {
            product.setCategoryId(dto.getCategoryId());
        }
        if (dto.getMainImage() != null) {
            product.setMainImage(dto.getMainImage());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getSpecification() != null) {
            product.setSpecification(dto.getSpecification());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }

        // 种子特有属性
        if (dto.getOrigin() != null) {
            product.setOrigin(dto.getOrigin());
        }
        if (dto.getVariety() != null) {
            product.setVariety(dto.getVariety());
        }
        if (dto.getDifficulty() != null) {
            product.setDifficulty(dto.getDifficulty());
        }
        if (dto.getGrowthCycle() != null) {
            product.setGrowthCycle(dto.getGrowthCycle());
        }
        if (dto.getGerminationRate() != null) {
            product.setGerminationRate(dto.getGerminationRate());
        }
        if (dto.getPurity() != null) {
            product.setPurity(dto.getPurity());
        }
        if (dto.getShelfLife() != null) {
            product.setShelfLife(dto.getShelfLife());
        }
        if (dto.getProductionDate() != null) {
            product.setProductionDate(dto.getProductionDate());
        }

        // 种植环境
        if (dto.getMinTemperature() != null) {
            product.setMinTemperature(dto.getMinTemperature());
        }
        if (dto.getMaxTemperature() != null) {
            product.setMaxTemperature(dto.getMaxTemperature());
        }
        if (dto.getMinHumidity() != null) {
            product.setMinHumidity(dto.getMinHumidity());
        }
        if (dto.getMaxHumidity() != null) {
            product.setMaxHumidity(dto.getMaxHumidity());
        }
        if (dto.getMinPh() != null) {
            product.setMinPh(dto.getMinPh());
        }
        if (dto.getMaxPh() != null) {
            product.setMaxPh(dto.getMaxPh());
        }
        if (dto.getLightRequirement() != null) {
            product.setLightRequirement(dto.getLightRequirement());
        }

        // JSON字段
        if (dto.getImages() != null) {
            product.setImages(JSON.toJSONString(dto.getImages()));
        }
        if (dto.getRegions() != null) {
            product.setRegions(JSON.toJSONString(dto.getRegions()));
        }
        if (dto.getPlantingSeasons() != null) {
            product.setPlantingSeasons(JSON.toJSONString(dto.getPlantingSeasons()));
        }

        // 质量溯源
        if (dto.getTraceCode() != null) {
            product.setTraceCode(dto.getTraceCode());
        }
        if (dto.getBatchNumber() != null) {
            product.setBatchNumber(dto.getBatchNumber());
        }
        if (dto.getInspectionReportUrl() != null) {
            product.setInspectionReportUrl(dto.getInspectionReportUrl());
        }
    }

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
     * 扣减商品库存
     * 使用乐观锁防止超卖
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否成功
     * @throws BusinessException 库存不足
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("扣减库存, productId: {}, quantity: {}", productId, quantity);
        
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        if (product.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        
        // 使用乐观锁更新，防止超卖
        int affected = productMapper.deductStock(productId, quantity, product.getStock());
        if (affected == 0) {
            throw new BusinessException("库存不足或已被他人购买");
        }
        
        log.info("扣减库存成功, productId: {}, quantity: {}, remaining: {}", 
                productId, quantity, product.getStock() - quantity);
        return true;
    }

    /**
     * 恢复商品库存
     * 用于订单取消或失败时回滚
     * 
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long productId, Integer quantity) {
        log.info("恢复库存, productId: {}, quantity: {}", productId, quantity);
        
        Product product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("商品不存在, 无法恢复库存, productId: {}", productId);
            return false;
        }
        
        product.setStock(product.getStock() + quantity);
        productMapper.updateById(product);
        
        log.info("恢复库存成功, productId: {}, quantity: {}, total: {}", 
                productId, quantity, product.getStock());
        return true;
    }

    /**
     * 搜索匹配候选商品
     * 根据品种和区域搜索上架商品，供异步匹配服务调用
     *
     * @param variety 品种关键词（可选）
     * @param region 区域关键词（可选）
     * @param limit 返回数量限制
     * @return 商品列表
     */
    public List<ProductVO> searchForMatch(String variety, String region, Integer limit) {
        log.info("搜索匹配候选商品: variety={}, region={}, limit={}", variety, region, limit);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询上架商品
        queryWrapper.eq(Product::getStatus, "ON_SALE");

        // 品种筛选
        if (StringUtils.hasText(variety)) {
            queryWrapper.like(Product::getVariety, variety);
        }

        // 区域筛选
        if (StringUtils.hasText(region)) {
            queryWrapper.like(Product::getRegions, region);
        }

        // 按销量降序，优先匹配热门商品
        queryWrapper.orderByDesc(Product::getSales);

        // 限制返回数量
        queryWrapper.last("LIMIT " + limit);

        List<Product> products = productMapper.selectList(queryWrapper);

        log.info("搜索到候选商品: {} 个", products.size());

        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 增加商品销量
     * 
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseSales(Long productId, Integer quantity) {
        log.info("增加销量, productId: {}, quantity: {}", productId, quantity);
        
        Product product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("商品不存在, 无法增加销量, productId: {}", productId);
            return false;
        }
        
        Integer currentSales = product.getSales() != null ? product.getSales() : 0;
        product.setSales(currentSales + quantity);
        productMapper.updateById(product);
        
        log.info("增加销量成功, productId: {}, quantity: {}, total: {}", 
                productId, quantity, product.getSales());
        return true;
    }

    /**
     * 将Product转换为ProductVO
     * 
     * @param product 商品实体
     * @return 商品VO
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        
        // 基础信息
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setCategoryId(product.getCategoryId());
        vo.setMainImage(product.getMainImage());
        vo.setDescription(product.getDescription());
        vo.setSpecification(product.getSpecification());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setSupplierId(product.getSupplierId());
        vo.setStatus(product.getStatus());
        
        // 种子特有属性
        vo.setOrigin(product.getOrigin());
        vo.setVariety(product.getVariety());
        vo.setDifficulty(product.getDifficulty());
        vo.setGrowthCycle(product.getGrowthCycle());
        vo.setGerminationRate(product.getGerminationRate());
        vo.setPurity(product.getPurity());
        vo.setShelfLife(product.getShelfLife());
        vo.setProductionDate(product.getProductionDate());
        
        // 种植环境要求
        vo.setMinTemperature(product.getMinTemperature());
        vo.setMaxTemperature(product.getMaxTemperature());
        vo.setMinHumidity(product.getMinHumidity());
        vo.setMaxHumidity(product.getMaxHumidity());
        vo.setMinPh(product.getMinPh());
        vo.setMaxPh(product.getMaxPh());
        vo.setLightRequirement(product.getLightRequirement());
        
        // 质量溯源
        vo.setTraceCode(product.getTraceCode());
        vo.setBatchNumber(product.getBatchNumber());
        vo.setInspectionReportUrl(product.getInspectionReportUrl());
        vo.setBlockchainHash(product.getBlockchainHash());
        
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
        
        if (StringUtils.hasText(product.getPlantingSeasons())) {
            vo.setPlantingSeasons(JSON.parseArray(product.getPlantingSeasons(), String.class));
        } else {
            vo.setPlantingSeasons(new ArrayList<>());
        }
        
        // 查询分类名称
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        
        return vo;
    }
}
