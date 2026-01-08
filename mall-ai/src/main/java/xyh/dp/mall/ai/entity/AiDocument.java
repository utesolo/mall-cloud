package xyh.dp.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档实体类
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("ai_document")
public class AiDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 文件存储路径
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 处理状态: PENDING-待处理, PROCESSING-处理中, COMPLETED-已完成, FAILED-处理失败
     */
    private String processingStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除标识: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
