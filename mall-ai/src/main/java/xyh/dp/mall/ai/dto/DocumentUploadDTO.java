package xyh.dp.mall.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档上传请求DTO
 * 用于上传PDF文档到知识库
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "文档上传请求")
public class DocumentUploadDTO {

    @NotNull(message = "文件不能为空")
    @Schema(description = "PDF文件", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

    @Schema(description = "文档名称（为空则使用文件名）", example = "技术文档.pdf")
    private String documentName;

    @Min(value = 100, message = "分块大小必须大于100")
    @Schema(description = "分块大小（token数）", example = "800")
    private Integer chunkSize;

    @Min(value = 0, message = "块重叠大小不能为负")
    @Schema(description = "块重叠大小", example = "50")
    private Integer chunkOverlap;
}
