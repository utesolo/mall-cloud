package xyh.dp.mall.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置类
 * 用于HTTP请求（如调用ML模型API）
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate Bean
     * 配置连接超时、读取超时等参数
     *
     * @param builder RestTemplateBuilder
     * @return 配置好的RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("初始化RestTemplate");
        
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    /**
     * 配置HTTP请求工厂
     * 支持请求/响应的日志记录
     *
     * @return ClientHttpRequestFactory实例
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);      // 连接超时：5秒
        factory.setReadTimeout(10000);        // 读取超时：10秒
        factory.setBufferRequestBody(true);   // 缓存请求体，便于日志记录
        
        return new BufferingClientHttpRequestFactory(factory);
    }
}
