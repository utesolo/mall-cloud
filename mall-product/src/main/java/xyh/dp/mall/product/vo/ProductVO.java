package xyh.dp.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品VO（种子类商品）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "商品信息")
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息 ====================

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "商品主图")
    private String mainImage;

    @Schema(description = "商品图片列表")
    private List<String> images;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "商品规格")
    private String specification;

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "商品状态")
    private String status;

    // ==================== 种子特有属性 ====================

    @Schema(description = "种子产地")
    private String origin;

    @Schema(description = "种子品种")
    private String variety;

    @Schema(description = "种植难度(EASY/MEDIUM/HARD)")
    private String difficulty;

    @Schema(description = "生长周期(天)")
    private Integer growthCycle;

    @Schema(description = "发芽率(%)")
    private BigDecimal germinationRate;

    @Schema(description = "品种纯度(%)")
    private BigDecimal purity;

    @Schema(description = "保质期(月)")
    private Integer shelfLife;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    // ==================== 种植环境要求 ====================

    @Schema(description = "适宜温度下限(℃)")
    private BigDecimal minTemperature;

    @Schema(description = "适宜温度上限(℃)")
    private BigDecimal maxTemperature;

    @Schema(description = "适宜湿度下限(%)")
    private BigDecimal minHumidity;

    @Schema(description = "适宜湿度上限(%)")
    private BigDecimal maxHumidity;

    @Schema(description = "适宜土壤PH下限")
    private BigDecimal minPh;

    @Schema(description = "适宜土壤PH上限")
    private BigDecimal maxPh;

    @Schema(description = "光照要求(FULL_SUN/PARTIAL_SUN/SHADE)")
    private String lightRequirement;

    @Schema(description = "适配区域")
    private List<String> regions;

    @Schema(description = "种植季节")
    private List<String> plantingSeasons;

    // ==================== 质量溯源 ====================

    @Schema(description = "溯源码")
    private String traceCode;

    @Schema(description = "生产批次号")
    private String batchNumber;

    @Schema(description = "质检报告URL")
    private String inspectionReportUrl;

    @Schema(description = "区块链存证哈希")
    private String blockchainHash;
}
