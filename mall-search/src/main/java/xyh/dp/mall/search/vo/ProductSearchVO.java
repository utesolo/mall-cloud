package xyh.dp.mall.search.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品搜索结果VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class ProductSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品图片列表
     */
    private List<String> images;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品规格
     */
    private String specification;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 商品状态
     */
    private String status;

    /**
     * 种子产地
     */
    private String origin;

    /**
     * 种子品种
     */
    private String variety;

    /**
     * 种植难度
     */
    private String difficulty;

    /**
     * 生长周期(天)
     */
    private Integer growthCycle;

    /**
     * 发芽率(%)
     */
    private BigDecimal germinationRate;

    /**
     * 品种纯度(%)
     */
    private BigDecimal purity;

    /**
     * 保质期(月)
     */
    private Integer shelfLife;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 适宜温度下限(℃)
     */
    private BigDecimal minTemperature;

    /**
     * 适宜温度上限(℃)
     */
    private BigDecimal maxTemperature;

    /**
     * 适宜湿度下限(%)
     */
    private BigDecimal minHumidity;

    /**
     * 适宜湿度上限(%)
     */
    private BigDecimal maxHumidity;

    /**
     * 适宜土壤PH下限
     */
    private BigDecimal minPh;

    /**
     * 适宜土壤PH上限
     */
    private BigDecimal maxPh;

    /**
     * 光照要求
     */
    private String lightRequirement;

    /**
     * 适配区域
     */
    private List<String> regions;

    /**
     * 种植季节
     */
    private List<String> plantingSeasons;

    /**
     * 溯源码
     */
    private String traceCode;

    /**
     * 生产批次号
     */
    private String batchNumber;

    /**
     * 质检报告URL
     */
    private String inspectionReportUrl;

    /**
     * 区块链存证哈希
     */
    private String blockchainHash;

    /**
     * 搜索评分(ES返回的相关度分数)
     */
    private Float score;
}
