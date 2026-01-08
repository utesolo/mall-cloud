package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档块引用VO
 * 用于展示引用的文档块信息
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档块引用")
public class ChunkReferenceVO {

    @Schema(description = "文档块ID")
    private Long chunkId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "页码")
    private Integer pageNumber;

    @Schema(description = "块内容摘要")
    private String content;

    @Schema(description = "相似度分数")
    private Double similarity;
}
