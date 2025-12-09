package xyh.dp.mall.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.auth.dto.WeChatLoginDTO;
import xyh.dp.mall.auth.service.AuthService;
import xyh.dp.mall.auth.vo.LoginVO;
import xyh.dp.mall.common.result.Result;

/**
 * 认证控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户登录、注册、退出等认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 微信小程序登录
     * 
     * @param loginDTO 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/wechat/login")
    @Operation(summary = "微信小程序登录", description = "通过微信code进行登录,新用户自动注册")
    public Result<LoginVO> weChatLogin(@RequestBody WeChatLoginDTO loginDTO) {
        log.info("微信登录请求: code={}, userType={}", loginDTO.getCode(), loginDTO.getUserType());
        LoginVO loginVO = authService.weChatLogin(loginDTO);
        return Result.success(loginVO, "登录成功");
    }

    /**
     * 退出登录
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "清除用户登录状态")
    public Result<Void> logout(@RequestParam Long userId) {
        log.info("用户退出登录: userId={}", userId);
        authService.logout(userId);
        return Result.success(null, "退出成功");
    }
}
