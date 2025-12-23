package xyh.dp.mall.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品ES文档
 * 存储在Elasticsearch中用于全文搜索和分词检索
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Document(indexName = "product")
public class ProductDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @Id
    private Long id;

    /**
     * 商品名称(使用IK分词器)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 商品分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 分类名称(使用IK分词器)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;

    /**
     * 商品主图
     */
    @Field(type = FieldType.Keyword)
    private String mainImage;

    /**
     * 商品图片列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> images;

    /**
     * 商品描述(使用IK分词器)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 商品规格
     */
    @Field(type = FieldType.Keyword)
    private String specification;

    /**
     * 商品价格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /**
     * 销量
     */
    @Field(type = FieldType.Integer)
    private Integer sales;

    /**
     * 供应商ID
     */
    @Field(type = FieldType.Long)
    private Long supplierId;

    /**
     * 商品状态
     */
    @Field(type = FieldType.Keyword)
    private String status;

    // ==================== 种子特有属性 ====================

    /**
     * 种子产地(使用IK分词器)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String origin;

    /**
     * 种子品种(使用IK分词器)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String variety;

    /**
     * 种植难度
     */
    @Field(type = FieldType.Keyword)
    private String difficulty;

    /**
     * 生长周期(天)
     */
    @Field(type = FieldType.Integer)
    private Integer growthCycle;

    /**
     * 发芽率(%)
     */
    @Field(type = FieldType.Double)
    private BigDecimal germinationRate;

    /**
     * 品种纯度(%)
     */
    @Field(type = FieldType.Double)
    private BigDecimal purity;

    /**
     * 保质期(月)
     */
    @Field(type = FieldType.Integer)
    private Integer shelfLife;

    /**
     * 生产日期
     */
    @Field(type = FieldType.Date)
    private LocalDate productionDate;

    // ==================== 种植环境要求 ====================

    /**
     * 适宜温度下限(℃)
     */
    @Field(type = FieldType.Double)
    private BigDecimal minTemperature;

    /**
     * 适宜温度上限(℃)
     */
    @Field(type = FieldType.Double)
    private BigDecimal maxTemperature;

    /**
     * 适宜湿度下限(%)
     */
    @Field(type = FieldType.Double)
    private BigDecimal minHumidity;

    /**
     * 适宜湿度上限(%)
     */
    @Field(type = FieldType.Double)
    private BigDecimal maxHumidity;

    /**
     * 适宜土壤PH下限
     */
    @Field(type = FieldType.Double)
    private BigDecimal minPh;

    /**
     * 适宜土壤PH上限
     */
    @Field(type = FieldType.Double)
    private BigDecimal maxPh;

    /**
     * 光照要求
     */
    @Field(type = FieldType.Keyword)
    private String lightRequirement;

    /**
     * 适配区域(支持多个区域搜索)
     */
    @Field(type = FieldType.Keyword)
    private List<String> regions;

    /**
     * 种植季节(支持多个季节搜索)
     */
    @Field(type = FieldType.Keyword)
    private List<String> plantingSeasons;

    // ==================== 质量溯源 ====================

    /**
     * 溯源码
     */
    @Field(type = FieldType.Keyword)
    private String traceCode;

    /**
     * 生产批次号
     */
    @Field(type = FieldType.Keyword)
    private String batchNumber;

    /**
     * 质检报告URL
     */
    @Field(type = FieldType.Keyword)
    private String inspectionReportUrl;

    /**
     * 区块链存证哈希
     */
    @Field(type = FieldType.Keyword)
    private String blockchainHash;

    // ==================== 时间戳 ====================

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
}
