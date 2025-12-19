package xyh.dp.mall.common.tracing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 链路追踪配置属性
 * 支持配置重要路径和非重要路径的差异化采样率
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tracing")
public class TracingProperties {

    /**
     * 默认采样率（非重要链路）
     * 默认0.1表示10%的请求会被采样
     */
    private float defaultProbability = 0.1f;

    /**
     * 重要链路采样率
     * 默认1.0表示100%采样
     */
    private float importantProbability = 1.0f;

    /**
     * 重要路径列表（支持前缀匹配）
     * 这些路径将使用100%采样率
     */
    private List<String> importantPaths = new ArrayList<>();

    /**
     * 重要服务列表
     * 这些服务的所有请求将使用100%采样率
     */
    private List<String> importantServices = new ArrayList<>();

    /**
     * 忽略的路径列表（不记录）
     * 如健康检查等
     */
    private List<String> ignorePaths = new ArrayList<>();
}
