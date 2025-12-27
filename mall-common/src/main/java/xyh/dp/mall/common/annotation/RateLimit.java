package xyh.dp.mall.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 用于标记需要限流的接口，防止脚本快速重复请求
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key的前缀
     * 实际key为: prefix:userId
     * 
     * @return 限流key前缀
     */
    String prefix() default "rate_limit";

    /**
     * 时间窗口（秒）
     * 默认60秒（1分钟）
     * 
     * @return 时间窗口
     */
    int window() default 60;

    /**
     * 时间窗口内最大请求次数
     * 默认50次
     * 
     * @return 最大请求次数
     */
    int maxRequests() default 50;

    /**
     * 限流提示信息
     * 
     * @return 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
