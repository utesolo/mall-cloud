package xyh.dp.mall.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买记录VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class PurchaseRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

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
     * 购买单价
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 购买总金额
     */
    private BigDecimal totalAmount;

    /**
     * 购买时间
     */
    private LocalDateTime purchaseTime;
}
