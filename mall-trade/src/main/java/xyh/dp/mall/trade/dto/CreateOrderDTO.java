package xyh.dp.mall.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "商品ID", required = true)
    private Long productId;

    @Schema(description = "购买数量", required = true)
    private Integer quantity;

    @Schema(description = "收货人姓名", required = true)
    private String receiverName;

    @Schema(description = "收货人电话", required = true)
    private String receiverPhone;

    @Schema(description = "收货地址", required = true)
    private String receiverAddress;

    @Schema(description = "备注")
    private String remark;
}
