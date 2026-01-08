package xyh.dp.mall.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI智能服务启动类
 * 提供基于RAG的文档智能问答功能，集成DashScope大模型和Qdrant向量数据库
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class MallAiApplication {

    /**
     * 启动AI智能服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallAiApplication.class, args);
    }
}
