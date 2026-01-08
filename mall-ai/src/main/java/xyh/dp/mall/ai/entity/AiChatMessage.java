package xyh.dp.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话消息实体类
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 角色: USER-用户消息, ASSISTANT-助手回复, SYSTEM-系统消息
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * Token消耗
     */
    private Integer tokenCount;

    /**
     * 引用的文档块ID列表（JSON格式）
     */
    private String referencedChunks;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
