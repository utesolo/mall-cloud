package xyh.dp.mall.common.tracing;

import brave.sampler.Sampler;
import brave.sampler.SamplerFunction;
import brave.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 智能采样器
 * 根据路径和服务类型进行差异化采样
 * - 重要路径：100%采样
 * - 普通路径：10%采样
 * - 忽略路径：不采样
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
public class SmartSampler implements SamplerFunction<HttpRequest> {

    private final TracingProperties properties;
    private final Sampler defaultSampler;
    private final Sampler importantSampler;

    /**
     * 构造函数
     *
     * @param properties 链路追踪配置属性
     */
    public SmartSampler(TracingProperties properties) {
        this.properties = properties;
        this.defaultSampler = Sampler.create(properties.getDefaultProbability());
        this.importantSampler = Sampler.create(properties.getImportantProbability());
        log.info("智能采样器初始化: 默认采样率={}, 重要链路采样率={}", 
                properties.getDefaultProbability(), properties.getImportantProbability());
    }

    /**
     * 判断请求是否需要采样
     *
     * @param request HTTP请求
     * @return 采样决策：true-采样，false-不采样，null-使用默认
     */
    @Override
    public Boolean trySample(HttpRequest request) {
        String path = request.path();
        String method = request.method();
        
        if (path == null) {
            return defaultSample();
        }

        // 忽略的路径不采样
        if (isIgnorePath(path)) {
            return false;
        }

        // 重要路径100%采样
        if (isImportantPath(path)) {
            log.debug("重要链路采样: {} {}", method, path);
            return importantSampler.isSampled(0L);
        }

        // 普通路径按默认采样率
        return defaultSample();
    }

    /**
     * 判断是否为忽略路径
     *
     * @param path 请求路径
     * @return 是否忽略
     */
    private boolean isIgnorePath(String path) {
        List<String> ignorePaths = properties.getIgnorePaths();
        if (ignorePaths == null || ignorePaths.isEmpty()) {
            // 默认忽略的路径
            return path.startsWith("/actuator") ||
                   path.startsWith("/health") ||
                   path.startsWith("/favicon") ||
                   path.equals("/");
        }
        
        for (String ignorePath : ignorePaths) {
            if (path.startsWith(ignorePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为重要路径
     *
     * @param path 请求路径
     * @return 是否重要
     */
    private boolean isImportantPath(String path) {
        List<String> importantPaths = properties.getImportantPaths();
        if (importantPaths == null || importantPaths.isEmpty()) {
            // 默认重要路径：订单、支付、匹配相关
            return path.startsWith("/order") ||
                   path.startsWith("/payment") ||
                   path.startsWith("/match") ||
                   path.contains("/create") ||
                   path.contains("/submit") ||
                   path.contains("/confirm") ||
                   path.contains("/cancel");
        }
        
        for (String importantPath : importantPaths) {
            if (path.startsWith(importantPath) || path.contains(importantPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 默认采样决策
     *
     * @return 采样决策
     */
    private boolean defaultSample() {
        return defaultSampler.isSampled(ThreadLocalRandom.current().nextLong());
    }
}
