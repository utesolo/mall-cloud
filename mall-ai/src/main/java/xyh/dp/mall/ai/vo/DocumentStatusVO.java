package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档处理状态VO
 * 用于返回文档处理进度和状态
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档处理状态")
public class DocumentStatusVO {

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "处理状态")
    private String processingStatus;

    @Schema(description = "处理进度百分比")
    private Integer progress;

    @Schema(description = "当前步骤描述")
    private String currentStep;

    @Schema(description = "错误信息")
    private String errorMessage;
}
