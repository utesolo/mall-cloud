package xyh.dp.mall.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 文档查询请求DTO
 * 用于分页查询文档列表
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "文档查询请求")
public class DocumentQueryDTO {

    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "文档名称（模糊查询）", example = "技术文档")
    private String documentName;

    @Schema(description = "处理状态过滤", example = "COMPLETED")
    private String processingStatus;
}
