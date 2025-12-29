package xyh.dp.mall.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应VO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "登录响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * Access Token（访问令牌，短期有效）
     */
    @Schema(description = "访问令牌（15分钟有效）")
    private String accessToken;
    
    /**
     * Refresh Token（刷新令牌，长期有效）
     */
    @Schema(description = "刷新令牌（7天有效）")
    private String refreshToken;
    
    /**
     * Access Token过期时间（秒）
     */
    @Schema(description = "访问令牌过期时间（秒）")
    private Long expiresIn;

    /**
     * JWT Token（兼容旧接口）
     * @deprecated 请使用accessToken
     */
    @Deprecated
    @Schema(description = "访问令牌（已废弃，请使用accessToken）")
    private String token;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickname;

    /**
     * 用户头像
     */
    @Schema(description = "用户头像URL")
    private String avatar;

    /**
     * 用户类型
     */
    @Schema(description = "用户类型(FARMER/SUPPLIER)")
    private String userType;

    /**
     * 是否新用户
     */
    @Schema(description = "是否新注册用户")
    private Boolean isNewUser;
}
