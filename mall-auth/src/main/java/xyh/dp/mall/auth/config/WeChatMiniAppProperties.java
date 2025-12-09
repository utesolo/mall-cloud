package xyh.dp.mall.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.mini-app")
public class WeChatMiniAppProperties {

    /**
     * 小程序AppID
     */
    private String appId;

    /**
     * 小程序AppSecret
     */
    private String appSecret;
}
