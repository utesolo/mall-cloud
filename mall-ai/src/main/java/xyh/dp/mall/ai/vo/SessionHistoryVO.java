package xyh.dp.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话历史VO
 * 用于返回会话的历史消息
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话历史")
public class SessionHistoryVO {

    @Schema(description = "会话标识")
    private String sessionKey;

    @Schema(description = "消息列表")
    private List<MessageVO> messages;
}
