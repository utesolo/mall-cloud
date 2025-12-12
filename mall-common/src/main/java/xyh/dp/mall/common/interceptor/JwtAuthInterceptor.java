package xyh.dp.mall.common.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import xyh.dp.mall.common.context.UserContext;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.util.JwtUtil;

/**
 * JWT认证拦截器
 * 解析请求头中的Token，将用户信息存入UserContext
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String jwtSecret;

    /**
     * 构造函数
     * 
     * @param jwtSecret JWT密钥
     */
    public JwtAuthInterceptor(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    /**
     * 请求前处理：解析Token并设置用户上下文
     * 
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return true-继续处理, false-中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = JwtUtil.parseToken(token, jwtSecret);
                
                UserContext userContext = new UserContext();
                userContext.setUserId(Long.valueOf(claims.getSubject()));
                userContext.setUserType((String) claims.get("userType"));
                
                // 如果Token中有businessUserId，也设置上
                if (claims.get("businessUserId") != null) {
                    userContext.setBusinessUserId((String) claims.get("businessUserId"));
                }
                
                UserContextHolder.setContext(userContext);
                log.debug("用户认证成功, userId: {}, userType: {}", 
                        userContext.getUserId(), userContext.getUserType());
            } catch (Exception e) {
                log.warn("Token解析失败: {}", e.getMessage());
                // Token无效时不阻止请求，让业务层决定是否需要登录
            }
        }
        
        return true;
    }

    /**
     * 请求完成后清理用户上下文
     * 
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    /**
     * 从请求头中提取Token
     * 
     * @param request 请求对象
     * @return Token字符串，如果不存在返回null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
