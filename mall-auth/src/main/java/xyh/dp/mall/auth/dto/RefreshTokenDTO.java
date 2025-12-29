package xyh.dp.mall.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新Token请求DTO
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Refresh Token
     */
    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌", required = true)
    private String refreshToken;
}
