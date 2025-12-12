package xyh.dp.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品实体（种子类商品）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息 ====================

    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品图片列表(JSON数组)
     */
    private String images;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品规格（如：50克/袋、100粒/包）
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
     * 商品状态: ON_SALE-上架, OFF_SALE-下架
     */
    private String status;

    // ==================== 种子特有属性 ====================

    /**
     * 种子产地
     */
    private String origin;

    /**
     * 种子品种
     */
    private String variety;

    /**
     * 种植难度: EASY-简单, MEDIUM-中等, HARD-困难
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

    // ==================== 种植环境要求（IOT/AI推荐用） ====================

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
     * 光照要求: FULL_SUN-全日照, PARTIAL_SUN-半日照, SHADE-耐阴
     */
    private String lightRequirement;

    /**
     * 适配区域(JSON数组，如: ["华北","华东","华中"])
     */
    private String regions;

    /**
     * 种植季节(JSON数组，如: ["春季","秋季"])
     */
    private String plantingSeasons;

    // ==================== 质量溯源 ====================

    /**
     * 溯源码/追溯编号
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

    // ==================== 时间戳 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
