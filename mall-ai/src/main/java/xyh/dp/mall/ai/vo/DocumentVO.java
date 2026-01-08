package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档信息VO
 * 用于返回文档基本信息
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档信息")
public class DocumentVO {

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "文件URL")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "总页数")
    private Integer totalPages;

    @Schema(description = "分块数量")
    private Integer chunkCount;

    @Schema(description = "处理状态")
    private String processingStatus;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private String createdTime;
}
