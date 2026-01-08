package xyh.dp.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent配置属性类
 * 配置智能体相关参数
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.agent")
public class AgentProperties {

    /**
     * 生成温度（0-2）
     */
    private Double temperature = 0.7;

    /**
     * 最大生成token数
     */
    private Integer maxTokens = 2000;

    /**
     * 会话过期时间（分钟）
     */
    private Integer sessionExpireMinutes = 30;
}
