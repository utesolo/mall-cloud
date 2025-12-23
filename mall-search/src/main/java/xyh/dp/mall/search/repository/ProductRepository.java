package xyh.dp.mall.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import xyh.dp.mall.search.document.ProductDocument;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品ES Repository
 * 提供基础的CRUD和自定义搜索方法
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * 根据名称模糊查询(全文搜索)
     *
     * @param name     商品名称
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByNameLike(String name, Pageable pageable);

    /**
     * 根据品种查询
     *
     * @param variety  品种
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByVarietyLike(String variety, Pageable pageable);

    /**
     * 根据产地查询
     *
     * @param origin   产地
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByOriginLike(String origin, Pageable pageable);

    /**
     * 根据区域查询
     *
     * @param region   区域
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByRegionsContaining(String region, Pageable pageable);

    /**
     * 根据分类ID查询
     *
     * @param categoryId 分类ID
     * @param pageable   分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 根据价格区间查询
     *
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 根据状态查询
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByStatus(String status, Pageable pageable);

    /**
     * 根据种植季节查询
     *
     * @param season   季节
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByPlantingSeasonsContaining(String season, Pageable pageable);

    /**
     * 根据种植难度查询
     *
     * @param difficulty 难度
     * @param pageable   分页参数
     * @return 分页结果
     */
    Page<ProductDocument> findByDifficulty(String difficulty, Pageable pageable);
}
