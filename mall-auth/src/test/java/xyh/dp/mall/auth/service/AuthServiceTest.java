package xyh.dp.mall.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import xyh.dp.mall.auth.config.JwtProperties;
import xyh.dp.mall.auth.config.WeChatMiniAppProperties;
import xyh.dp.mall.auth.dto.WeChatLoginDTO;
import xyh.dp.mall.auth.entity.User;
import xyh.dp.mall.auth.mapper.UserMapper;
import xyh.dp.mall.auth.vo.LoginVO;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthService 认证服务单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 认证服务测试")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WeChatMiniAppProperties weChatProperties;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private WeChatLoginDTO loginDTO;
    private User existingUser;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        loginDTO = new WeChatLoginDTO();
        loginDTO.setCode("test_code_123");
        loginDTO.setNickname("测试用户");
        loginDTO.setAvatar("https://example.com/avatar.jpg");
        loginDTO.setUserType("FARMER");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setOpenid("test_openid_123");
        existingUser.setNickname("老用户");
        existingUser.setAvatar("https://example.com/old_avatar.jpg");
        existingUser.setUserType("FARMER");
        existingUser.setStatus("NORMAL");
        existingUser.setCreateTime(LocalDateTime.now().minusDays(30));
        existingUser.setUpdateTime(LocalDateTime.now().minusDays(1));
    }

    @Nested
    @DisplayName("logout 退出登录测试")
    class LogoutTest {

        /**
         * 测试正常退出登录
         */
        @Test
        @DisplayName("正常退出登录应删除Redis中的token")
        void logout_shouldDeleteTokenFromRedis() {
            // Given
            Long userId = 1L;

            // When
            authService.logout(userId);

            // Then
            verify(redisTemplate, times(1)).delete(eq("auth:token:" + userId));
        }

        /**
         * 测试用户ID为空时退出登录
         */
        @Test
        @DisplayName("用户ID为null时应正常执行")
        void logout_withNullUserId_shouldStillExecute() {
            // Given
            Long userId = null;

            // When
            authService.logout(userId);

            // Then
            verify(redisTemplate, times(1)).delete(eq("auth:token:null"));
        }
    }

    @Nested
    @DisplayName("LoginVO 构建测试")
    class LoginVOBuildTest {

        /**
         * 测试LoginVO包含所有必要字段
         */
        @Test
        @DisplayName("LoginVO应包含用户基本信息")
        void loginVO_shouldContainUserInfo() {
            // Given
            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(1L);
            loginVO.setToken("test_token");
            loginVO.setNickname("测试用户");
            loginVO.setAvatar("https://example.com/avatar.jpg");
            loginVO.setUserType("FARMER");
            loginVO.setIsNewUser(false);

            // Then
            assertThat(loginVO.getUserId()).isEqualTo(1L);
            assertThat(loginVO.getToken()).isEqualTo("test_token");
            assertThat(loginVO.getNickname()).isEqualTo("测试用户");
            assertThat(loginVO.getAvatar()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(loginVO.getUserType()).isEqualTo("FARMER");
            assertThat(loginVO.getIsNewUser()).isFalse();
        }

        /**
         * 测试新用户标识
         */
        @Test
        @DisplayName("新用户isNewUser应为true")
        void loginVO_newUser_shouldHaveIsNewUserTrue() {
            // Given
            LoginVO loginVO = new LoginVO();
            loginVO.setIsNewUser(true);

            // Then
            assertThat(loginVO.getIsNewUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("User 实体测试")
    class UserEntityTest {

        /**
         * 测试用户实体字段设置
         */
        @Test
        @DisplayName("用户实体应正确设置所有字段")
        void user_shouldSetAllFields() {
            // Given
            User user = new User();
            user.setId(1L);
            user.setOpenid("openid_123");
            user.setNickname("测试昵称");
            user.setAvatar("avatar_url");
            user.setUserType("SUPPLIER");
            user.setStatus("NORMAL");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            // Then
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getOpenid()).isEqualTo("openid_123");
            assertThat(user.getNickname()).isEqualTo("测试昵称");
            assertThat(user.getAvatar()).isEqualTo("avatar_url");
            assertThat(user.getUserType()).isEqualTo("SUPPLIER");
            assertThat(user.getStatus()).isEqualTo("NORMAL");
            assertThat(user.getCreateTime()).isNotNull();
            assertThat(user.getUpdateTime()).isNotNull();
        }

        /**
         * 测试不同用户类型
         */
        @Test
        @DisplayName("用户类型应支持FARMER和SUPPLIER")
        void user_shouldSupportDifferentUserTypes() {
            // Given
            User farmer = new User();
            farmer.setUserType("FARMER");

            User supplier = new User();
            supplier.setUserType("SUPPLIER");

            // Then
            assertThat(farmer.getUserType()).isEqualTo("FARMER");
            assertThat(supplier.getUserType()).isEqualTo("SUPPLIER");
        }
    }

    @Nested
    @DisplayName("WeChatLoginDTO 验证测试")
    class WeChatLoginDTOTest {

        /**
         * 测试登录DTO字段设置
         */
        @Test
        @DisplayName("登录DTO应正确设置所有字段")
        void loginDTO_shouldSetAllFields() {
            // Given
            WeChatLoginDTO dto = new WeChatLoginDTO();
            dto.setCode("wx_code_123");
            dto.setNickname("微信用户");
            dto.setAvatar("wx_avatar_url");
            dto.setUserType("FARMER");

            // Then
            assertThat(dto.getCode()).isEqualTo("wx_code_123");
            assertThat(dto.getNickname()).isEqualTo("微信用户");
            assertThat(dto.getAvatar()).isEqualTo("wx_avatar_url");
            assertThat(dto.getUserType()).isEqualTo("FARMER");
        }
    }
}
