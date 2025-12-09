package xyh.dp.mall.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "订单信息")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品主图")
    private String productImage;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人电话")
    private String receiverPhone;

    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
}
