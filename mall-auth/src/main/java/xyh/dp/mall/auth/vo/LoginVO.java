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
     * JWT Token
     */
    @Schema(description = "访问令牌")
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
