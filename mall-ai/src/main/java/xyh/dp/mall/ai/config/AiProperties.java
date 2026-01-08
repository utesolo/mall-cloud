package xyh.dp.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置属性类
 * 从Nacos配置中心读取AI相关配置
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai")
public class AiProperties {

    /**
     * DashScope配置
     */
    private DashScopeConfig dashscope = new DashScopeConfig();

    /**
     * Qdrant配置
     */
    private QdrantConfig qdrant = new QdrantConfig();

    /**
     * PyMuPDF服务配置
     */
    private PyMuPDFConfig pymupdf = new PyMuPDFConfig();

    @Data
    public static class DashScopeConfig {
        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 对话模型
         */
        private String model = "qwen-plus";

        /**
         * 向量化模型
         */
        private String embeddingModel = "text-embedding-v2";
    }

    @Data
    public static class QdrantConfig {
        /**
         * Qdrant服务地址
         */
        private String host = "localhost";

        /**
         * Qdrant服务端口
         */
        private Integer port = 6333;

        /**
         * 向量集合名称
         */
        private String collectionName = "mall_documents";

        /**
         * 向量维度
         */
        private Integer vectorSize = 1536;

        /**
         * 是否使用HTTPS
         */
        private Boolean useTls = false;
    }

    @Data
    public static class PyMuPDFConfig {
        /**
         * PyMuPDF服务URL
         */
        private String serviceUrl = "http://localhost:5000";

        /**
         * 请求超时时间（秒）
         */
        private Integer timeout = 30;
    }
}
