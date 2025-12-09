package xyh.dp.mall.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录请求DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "微信登录请求")
public class WeChatLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 微信登录code
     */
    @Schema(description = "微信登录code", required = true)
    private String code;

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
     * 用户类型: FARMER-农户, SUPPLIER-供销商
     */
    @Schema(description = "用户类型(FARMER/SUPPLIER)")
    private String userType;
}
