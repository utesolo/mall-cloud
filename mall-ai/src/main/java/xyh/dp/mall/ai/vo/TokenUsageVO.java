package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用情况VO
 * 用于展示大模型Token消耗信息
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token使用情况")
public class TokenUsageVO {

    @Schema(description = "提示Token数")
    private Integer promptTokens;

    @Schema(description = "完成Token数")
    private Integer completionTokens;

    @Schema(description = "总Token数")
    private Integer totalTokens;
}
