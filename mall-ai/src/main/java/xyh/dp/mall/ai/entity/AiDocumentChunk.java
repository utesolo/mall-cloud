package xyh.dp.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档块实体类
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("ai_document_chunk")
public class AiDocumentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 块ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 块序号
     */
    private Integer chunkIndex;

    /**
     * 块内容
     */
    private String content;

    /**
     * Token数量
     */
    private Integer tokenCount;

    /**
     * 所在页码
     */
    private Integer pageNumber;

    /**
     * 向量ID（Qdrant中的ID）
     */
    private String vectorId;

    /**
     * 额外元数据（JSON格式）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
