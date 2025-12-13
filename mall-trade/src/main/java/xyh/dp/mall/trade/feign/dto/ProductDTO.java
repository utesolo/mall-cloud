package xyh.dp.mall.trade.feign.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

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
}
