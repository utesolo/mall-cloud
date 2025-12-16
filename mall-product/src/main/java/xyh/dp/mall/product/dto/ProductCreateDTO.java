package xyh.dp.mall.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品创建请求DTO
 * 用于商家新增商品时的参数封装
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "商品创建请求")
public class ProductCreateDTO {

    // ==================== 基础信息 ====================

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100")
    @Schema(description = "商品名称", example = "优质番茄种子", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "商品分类不能为空")
    @Schema(description = "商品分类ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "商品主图URL", example = "https://cdn.example.com/product/tomato.jpg")
    private String mainImage;

    @Schema(description = "商品图片列表")
    private List<String> images;

    @Size(max = 1000, message = "商品描述长度不能超过1000")
    @Schema(description = "商品描述", example = "精选优质番茄种子，抗病性强，产量高")
    private String description;

    @Schema(description = "商品规格", example = "50克/袋")
    private String specification;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Schema(description = "商品价格", example = "25.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @Schema(description = "库存数量", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    // ==================== 种子特有属性 ====================

    @Schema(description = "种子产地", example = "山东寿光")
    private String origin;

    @Schema(description = "种子品种", example = "番茄")
    private String variety;

    @Schema(description = "种植难度: EASY-简单, MEDIUM-中等, HARD-困难", example = "EASY")
    private String difficulty;

    @Min(value = 1, message = "生长周期必须大于0")
    @Schema(description = "生长周期(天)", example = "90")
    private Integer growthCycle;

    @DecimalMin(value = "0", message = "发芽率不能为负")
    @DecimalMax(value = "100", message = "发芽率不能超过100")
    @Schema(description = "发芽率(%)", example = "95.5")
    private BigDecimal germinationRate;

    @DecimalMin(value = "0", message = "纯度不能为负")
    @DecimalMax(value = "100", message = "纯度不能超过100")
    @Schema(description = "品种纯度(%)", example = "99.0")
    private BigDecimal purity;

    @Min(value = 1, message = "保质期必须大于0")
    @Schema(description = "保质期(月)", example = "24")
    private Integer shelfLife;

    @Schema(description = "生产日期", example = "2024-01-15")
    private LocalDate productionDate;

    // ==================== 种植环境要求 ====================

    @Schema(description = "适宜温度下限(℃)", example = "15.0")
    private BigDecimal minTemperature;

    @Schema(description = "适宜温度上限(℃)", example = "30.0")
    private BigDecimal maxTemperature;

    @Schema(description = "适宜湿度下限(%)", example = "40.0")
    private BigDecimal minHumidity;

    @Schema(description = "适宜湿度上限(%)", example = "80.0")
    private BigDecimal maxHumidity;

    @Schema(description = "适宜土壤PH下限", example = "6.0")
    private BigDecimal minPh;

    @Schema(description = "适宜土壤PH上限", example = "7.5")
    private BigDecimal maxPh;

    @Schema(description = "光照要求: FULL_SUN-全日照, PARTIAL_SUN-半日照, SHADE-耐阴", example = "FULL_SUN")
    private String lightRequirement;

    @Schema(description = "适配区域列表", example = "[\"华北\", \"华东\", \"华中\"]")
    private List<String> regions;

    @Schema(description = "种植季节列表", example = "[\"春季\", \"夏季\"]")
    private List<String> plantingSeasons;

    // ==================== 质量溯源 ====================

    @Schema(description = "溯源码/追溯编号", example = "TRACE202401150001")
    private String traceCode;

    @Schema(description = "生产批次号", example = "BATCH202401")
    private String batchNumber;

    @Schema(description = "质检报告URL", example = "https://cdn.example.com/report/qc001.pdf")
    private String inspectionReportUrl;
}
