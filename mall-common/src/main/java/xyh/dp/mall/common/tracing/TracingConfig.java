package xyh.dp.mall.common.tracing;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.sampler.Sampler;
import brave.sampler.SamplerFunction;
import brave.http.HttpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪配置类
 * 配置智能采样器，实现差异化采样
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnClass(Tracing.class)
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    private final TracingProperties tracingProperties;

    /**
     * 创建HTTP请求采样器
     * 根据请求路径进行差异化采样
     *
     * @return HTTP采样器函数
     */
    @Bean
    @ConditionalOnMissingBean
    public SamplerFunction<HttpRequest> httpSampler() {
        log.info("配置智能HTTP采样器");
        return new SmartSampler(tracingProperties);
    }

    /**
     * 创建默认采样器
     * 用于非HTTP请求的采样
     *
     * @return 默认采样器
     */
    @Bean
    @ConditionalOnMissingBean
    public Sampler defaultSampler() {
        float probability = tracingProperties.getDefaultProbability();
        log.info("配置默认采样器: probability={}", probability);
        return Sampler.create(probability);
    }
}
