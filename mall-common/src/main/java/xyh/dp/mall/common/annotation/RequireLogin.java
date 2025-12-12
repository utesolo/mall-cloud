package xyh.dp.mall.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录检查注解
 * 标记在方法或类上，表示该接口需要登录后才能访问
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {

    /**
     * 是否必须登录，默认为true
     * 设为false时仅解析Token但不强制登录
     * 
     * @return 是否必须登录
     */
    boolean required() default true;

    /**
     * 允许的用户类型，为空表示不限制
     * 
     * @return 允许的用户类型数组
     */
    String[] allowedTypes() default {};
}
