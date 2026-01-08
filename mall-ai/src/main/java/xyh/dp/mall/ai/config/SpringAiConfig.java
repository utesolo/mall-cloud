package xyh.dp.mall.ai.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Spring AI配置类
 * 配置DashScope、Qdrant等AI相关Bean
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SpringAiConfig {

    private final AiProperties aiProperties;

    /**
     * 配置Qdrant客户端
     * 用于向量存储和检索
     * 
     * @return Qdrant客户端实例
     */
    @Bean
    public QdrantClient qdrantClient() {
        log.info("初始化Qdrant客户端, host: {}, port: {}", 
                aiProperties.getQdrant().getHost(), 
                aiProperties.getQdrant().getPort());
        
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                aiProperties.getQdrant().getHost(),
                aiProperties.getQdrant().getPort(),
                aiProperties.getQdrant().getUseTls()
        );
        
        return new QdrantClient(builder.build());
    }

    /**
     * 配置PyMuPDF服务WebClient
     * 用于调用Python PDF解析服务
     * 
     * @return WebClient实例
     */
    @Bean
    public WebClient pyMuPDFWebClient() {
        log.info("初始化PyMuPDF WebClient, serviceUrl: {}", 
                aiProperties.getPymupdf().getServiceUrl());
        
        return WebClient.builder()
                .baseUrl(aiProperties.getPymupdf().getServiceUrl())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    // TODO: 配置DashScope相关Bean
    // Spring AI的DashScope集成可能需要根据实际版本调整
    // 这里预留配置位置，实际使用时需要根据Spring AI版本补充
}
