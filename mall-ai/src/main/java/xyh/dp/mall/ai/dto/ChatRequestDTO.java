package xyh.dp.mall.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Agent对话请求DTO
 * 用于发起对话和问答
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "Agent对话请求")
public class ChatRequestDTO {

    @Schema(description = "会话标识（为空则创建新会话）", example = "session_abc123")
    private String sessionKey;

    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户问题", example = "什么是微服务架构？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "限定查询的文档ID列表")
    private List<Long> documentIds;

    @Min(value = 1, message = "检索数量必须大于0")
    @Schema(description = "检索文档数量", example = "5")
    private Integer topK = 5;

    @DecimalMin(value = "0", message = "温度值必须大于等于0")
    @DecimalMax(value = "2", message = "温度值必须小于等于2")
    @Schema(description = "生成温度（控制随机性）", example = "0.7")
    private Double temperature = 0.7;
}
