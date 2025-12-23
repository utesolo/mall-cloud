package xyh.dp.mall.search.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索请求DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class ProductSearchDTO {

    /**
     * 关键词(支持商品名称、描述、品种、产地的全文搜索)
     */
    private String keyword;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 品种
     */
    private String variety;

    /**
     * 产地
     */
    private String origin;

    /**
     * 区域
     */
    private String region;

    /**
     * 种植季节
     */
    private String season;

    /**
     * 种植难度
     */
    private String difficulty;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 最低发芽率
     */
    private BigDecimal minGerminationRate;

    /**
     * 最低品种纯度
     */
    private BigDecimal minPurity;

    /**
     * 排序字段: price-价格, sales-销量, createTime-上架时间
     */
    private String sortField;

    /**
     * 排序方向: asc-升序, desc-降序
     */
    private String sortOrder;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
