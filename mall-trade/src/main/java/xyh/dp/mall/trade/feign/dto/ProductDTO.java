package xyh.dp.mall.trade.feign.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品信息DTO
 * 用于Feign调用商品服务时传输商品数据
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class ProductDTO implements Serializable {

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
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 商品状态: ON_SALE-上架, OFF_SALE-下架
     */
    private String status;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 种子品种
     */
    private String variety;

    /**
     * 种子产地
     */
    private String origin;

    // ==================== 种植环境要求（用于匹配） ====================

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
     * 光照要求: FULL_SUN-全日照, PARTIAL_SUN-半日照, SHADE-耐阴
     */
    private String lightRequirement;

    /**
     * 适配区域(如: ["华北","华东","华中","华南"，“华西”])
     */
    private List<String> regions;

    /**
     * 种植季节(如: ["春季","秋季"])
     */
    private List<String> plantingSeasons;

    // ==================== 种子质量指标（用于匹配） ====================

    /**
     * 发芽率(%)
     */
    private BigDecimal germinationRate;

    /**
     * 品种纯度(%)
     */
    private BigDecimal purity;

    /**
     * 种植难度: EASY-简单, MEDIUM-中等, HARD-困难
     */
    private String difficulty;

    /**
     * 生长周期(天)
     */
    private Integer growthCycle;

    /**
     * 商品描述
     */
    private String description;
}
