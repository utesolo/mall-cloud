package xyh.dp.mall.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * Access Token过期时间(毫秒)
     * 默认15分钟
     */
    private Long accessTokenExpiration = 900000L;

    /**
     * Refresh Token过期时间(毫秒)
     * 默认7天
     */
    private Long refreshTokenExpiration = 604800000L;
    
    /**
     * 兼容旧配置
     * @deprecated 请使用accessTokenExpiration
     */
    @Deprecated
    public Long getExpiration() {
        return accessTokenExpiration;
    }
    
    /**
     * 兼容旧配置
     * @deprecated 请使用setAccessTokenExpiration
     */
    @Deprecated
    public void setExpiration(Long expiration) {
        this.accessTokenExpiration = expiration;
    }
}
