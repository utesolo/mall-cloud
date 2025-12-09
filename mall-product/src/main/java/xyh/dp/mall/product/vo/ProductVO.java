package xyh.dp.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "商品信息")
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "适配区域")
    private List<String> regions;

    @Schema(description = "种植难度(EASY/MEDIUM/HARD)")
    private String difficulty;

    @Schema(description = "生长周期(天)")
    private Integer growthCycle;

    @Schema(description = "商品状态")
    private String status;
}
