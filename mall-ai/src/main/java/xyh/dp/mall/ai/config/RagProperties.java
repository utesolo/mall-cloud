package xyh.dp.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG配置属性类
 * 配置检索增强生成相关参数
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /**
     * 文档分块大小（token数）
     */
    private Integer chunkSize = 800;

    /**
     * 块重叠大小
     */
    private Integer chunkOverlap = 50;

    /**
     * 默认检索数量
     */
    private Integer topK = 5;

    /**
     * 相似度阈值（0-1）
     */
    private Double similarityThreshold = 0.7;
}
