package xyh.dp.mall.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销商品VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class HotProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排名（1开始）
     */
    private Integer rank;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品主图
     */
    private String productImage;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品分类名称
     */
    private String categoryName;

    /**
     * 商品品种
     */
    private String variety;

    /**
     * 商品产地
     */
    private String origin;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 统计周期内的销量
     */
    private Integer weekSales;

    /**
     * 统计周期内的购买人数
     */
    private Integer weekBuyerCount;

    /**
     * 统计周期内的总销售额
     */
    private BigDecimal weekTotalAmount;

    /**
     * 供应商ID
     */
    private Long supplierId;
}
