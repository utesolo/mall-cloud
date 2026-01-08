package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent对话响应VO
 * 用于返回AI助手的回答结果
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent对话响应")
public class ChatResponseVO {

    @Schema(description = "会话标识")
    private String sessionKey;

    @Schema(description = "助手回答")
    private String answer;

    @Schema(description = "引用的文档块列表")
    private List<ChunkReferenceVO> references;

    @Schema(description = "Token使用情况")
    private TokenUsageVO tokenUsage;
}
