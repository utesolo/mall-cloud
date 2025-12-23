package xyh.dp.mall.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新购物车数量DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class UpdateCartItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 购物车项ID
     */
    @NotNull(message = "购物车项ID不能为空")
    private Long id;

    /**
     * 新数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}
