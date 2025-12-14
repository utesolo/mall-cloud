package xyh.dp.mall.trade.tracking.annotation;

import java.lang.annotation.*;

/**
 * 埋点注解
 * 用于标记需要埋点的方法
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackEvent {

    /**
     * 事件类型
     * 
     * @return 事件类型
     */
    String eventType();

    /**
     * 事件描述
     * 
     * @return 事件描述
     */
    String description() default "";

    /**
     * 是否记录方法参数
     * 
     * @return 是否记录
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回值
     * 
     * @return 是否记录
     */
    boolean recordResult() default false;
}
