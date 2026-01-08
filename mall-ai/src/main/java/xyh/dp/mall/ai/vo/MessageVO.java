package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息VO
 * 用于展示单条对话消息
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息")
public class MessageVO {

    @Schema(description = "角色：USER-用户, ASSISTANT-助手")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private String createdTime;
}
