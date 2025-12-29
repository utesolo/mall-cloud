package xyh.dp.mall.auth.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import xyh.dp.mall.auth.config.JwtProperties;
import xyh.dp.mall.auth.config.WeChatMiniAppProperties;
import xyh.dp.mall.auth.dto.WeChatLoginDTO;
import xyh.dp.mall.auth.entity.User;
import xyh.dp.mall.auth.mapper.UserMapper;
import xyh.dp.mall.auth.vo.LoginVO;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final WeChatMiniAppProperties weChatProperties;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final TokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WECHAT_AUTH_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * 微信小程序登录
     * 
     * @param loginDTO 登录请求参数
     * @return 登录结果
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO weChatLogin(WeChatLoginDTO loginDTO) {
        // TODO 防止空指针异常
        // 1. 调用微信接口获取openid
        if (Objects.equals(loginDTO.getUserType(), "ADMIN")){

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getOpenid, "ADMIN");
            User user = userMapper.selectOne(queryWrapper);

        // 管理员登录测试
            Map<String, Object> tokens = tokenService.generateTokens(user);

            // 6. 构造返回结果
            return buildLoginVO(user, tokens, false);
        }
        String openid = getOpenIdFromWeChat(loginDTO.getCode());
        
        // 2. 查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(queryWrapper);
        
        boolean isNewUser = false;
        
        // 3. 如果不存在则创建新用户
        if (user == null) {
            user = createNewUser(openid, loginDTO);
            isNewUser = true;
        } else {
            // 更新用户信息
            updateUserInfo(user, loginDTO);
        }
        
        // 4. 生成双Token
        Map<String, Object> tokens = tokenService.generateTokens(user);
        
        // 5. 构造返回结果
        return buildLoginVO(user, tokens, isNewUser);
    }

    /**
     * 从微信获取OpenID
     * 
     * @param code 微信登录code
     * @return OpenID
     * @throws BusinessException 获取失败抛出异常
     */
    private String getOpenIdFromWeChat(String code) {
        String url = String.format(WECHAT_AUTH_URL, 
                weChatProperties.getAppId(), 
                weChatProperties.getAppSecret(), 
                code);
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject jsonObject = JSON.parseObject(response);
            
            if (jsonObject.containsKey("errcode")) {
                Integer errcode = jsonObject.getInteger("errcode");
                String errmsg = jsonObject.getString("errmsg");
                log.error("微信登录失败, errcode: {}, errmsg: {}", errcode, errmsg);
                throw new BusinessException("微信登录失败: " + errmsg);
            }
            
            return jsonObject.getString("openid");
        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new BusinessException("微信登录失败,请稍后重试");
        }
    }

    /**
     * 创建新用户
     * 
     * @param openid 微信OpenID
     * @param loginDTO 登录信息
     * @return 新用户
     */
    private User createNewUser(String openid, WeChatLoginDTO loginDTO) {
        User user = new User();
        user.setOpenid(openid);
        user.setNickname(loginDTO.getNickname());
        user.setAvatar(loginDTO.getAvatar());
        user.setUserType(loginDTO.getUserType());
        user.setStatus("NORMAL");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        log.info("创建新用户成功, userId: {}, openid: {}", user.getId(), openid);
        return user;
    }

    /**
     * 更新用户信息
     * 
     * @param user 用户对象
     * @param loginDTO 登录信息
     */
    private void updateUserInfo(User user, WeChatLoginDTO loginDTO) {
        boolean needUpdate = false;
        
        if (loginDTO.getNickname() != null && !loginDTO.getNickname().equals(user.getNickname())) {
            user.setNickname(loginDTO.getNickname());
            needUpdate = true;
        }
        
        if (loginDTO.getAvatar() != null && !loginDTO.getAvatar().equals(user.getAvatar())) {
            user.setAvatar(loginDTO.getAvatar());
            needUpdate = true;
        }
        
        if (needUpdate) {
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }
    
    /**
     * 构造登录返回结果
     * 
     * @param user 用户对象
     * @param tokens Token信息Map
     * @param isNewUser 是否新用户
     * @return 登录结果
     */
    private LoginVO buildLoginVO(User user, Map<String, Object> tokens, boolean isNewUser) {
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setAccessToken((String) tokens.get("accessToken"));
        loginVO.setRefreshToken((String) tokens.get("refreshToken"));
        loginVO.setExpiresIn((Long) tokens.get("expiresIn"));
        loginVO.setToken((String) tokens.get("accessToken")); // 兼容旧接口
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setUserType(user.getUserType());
        loginVO.setIsNewUser(isNewUser);
        return loginVO;
    }

    /**
     * 刷新Access Token
     * 使用Refresh Token换取新的Access Token
     * 
     * @param refreshToken Refresh Token
     * @return Token信息Map
     * @throws BusinessException Token无效或已过期
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        return tokenService.refreshAccessToken(refreshToken);
    }

    /**
     * 退出登录
     * 撤销用户的所有Token
     * 
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        tokenService.revokeAllTokens(userId);
        log.info("用户退出登录, userId: {}", userId);
    }
}
