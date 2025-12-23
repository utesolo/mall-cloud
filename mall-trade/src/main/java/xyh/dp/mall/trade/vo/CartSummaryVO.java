package xyh.dp.mall.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车汇总VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class CartSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 购物车项列表
     */
    private List<CartItemVO> items;

    /**
     * 总数量
     */
    private Integer totalQuantity;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 已选中的数量
     */
    private Integer selectedQuantity;

    /**
     * 已选中的总金额
     */
    private BigDecimal selectedAmount;
}
