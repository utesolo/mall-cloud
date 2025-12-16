package xyh.dp.mall.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存更新请求DTO
 * 用于商家调整商品库存时的参数封装
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "库存更新请求")
public class StockUpdateDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型: SET-直接设置, ADD-增加, SUBTRACT-减少", example = "SET", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operationType;

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    @Schema(description = "操作数量", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "操作备注", example = "补充库存")
    private String remark;

    /**
     * 库存操作类型枚举
     */
    public enum OperationType {
        /**
         * 直接设置库存
         */
        SET,
        /**
         * 增加库存
         */
        ADD,
        /**
         * 减少库存
         */
        SUBTRACT
    }
}
