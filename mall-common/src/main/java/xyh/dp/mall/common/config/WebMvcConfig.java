package xyh.dp.mall.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyh.dp.mall.common.interceptor.JwtAuthInterceptor;

/**
 * Web MVC配置
 * 注册JWT认证拦截器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${jwt.secret:default-jwt-secret-key-at-least-256-bits-long}")
    private String jwtSecret;

    /**
     * 注册拦截器
     * 
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtSecret))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",           // 认证相关接口不需要Token
                        "/doc.html",          // Swagger文档
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
