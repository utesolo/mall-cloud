package xyh.dp.mall.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建种植计划DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "创建种植计划请求")
public class CreatePlantingPlanDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "农户ID（自动从登录信息获取，无需传递）", example = "FARMER001")
    private String farmerId;

    @Schema(description = "种植面积(亩)", required = true, example = "30.0")
    @NotNull(message = "种植面积不能为空")
    @DecimalMin(value = "0.1", message = "种植面积必须大于0")
    private BigDecimal plantingArea;

    @Schema(description = "种植品种", required = true, example = "鹤首葫芦")
    @NotBlank(message = "种植品种不能为空")
    private String variety;

    @Schema(description = "预计产量(个)", required = true, example = "10000")
    @NotNull(message = "预计产量不能为空")
    @Min(value = 1, message = "预计产量必须大于0")
    private Integer expectedYield;

    @Schema(description = "种植时间", required = true, example = "2023-04-10")
    @NotNull(message = "种植时间不能为空")
    private LocalDate plantingDate;

    @Schema(description = "目标用途", required = true, example = "工艺品制作")
    @NotBlank(message = "目标用途不能为空")
    private String targetUsage;

    @Schema(description = "种植区域", required = true, example = "山东菏泽")
    @NotBlank(message = "种植区域不能为空")
    private String region;
}
