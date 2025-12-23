package xyh.dp.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买记录实体
 * 用于记录农户的购买历史,支持数据分析和推荐
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("tb_purchase_record")
public class PurchaseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 购买时间（订单支付时间）
     */
    private LocalDateTime purchaseTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
