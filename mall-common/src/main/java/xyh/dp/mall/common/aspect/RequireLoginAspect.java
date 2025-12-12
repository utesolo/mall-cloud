package xyh.dp.mall.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.annotation.RequireLogin;
import xyh.dp.mall.common.context.UserContext;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.exception.BusinessException;

import java.util.Arrays;

/**
 * 登录检查切面
 * 处理@RequireLogin注解，验证用户登录状态和权限
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class RequireLoginAspect {

    /**
     * 处理方法级别的@RequireLogin注解
     * 
     * @param joinPoint 切入点
     * @param requireLogin 登录注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(requireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint, RequireLogin requireLogin) throws Throwable {
        doCheck(requireLogin, joinPoint);
        return joinPoint.proceed();
    }

    /**
     * 处理类级别的@RequireLogin注解
     * 
     * @param joinPoint 切入点
     * @param requireLogin 登录注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@within(requireLogin)")
    public Object checkLoginOnClass(ProceedingJoinPoint joinPoint, RequireLogin requireLogin) throws Throwable {
        // 如果方法上也有注解，以方法注解为准
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireLogin methodAnnotation = signature.getMethod().getAnnotation(RequireLogin.class);
        if (methodAnnotation != null) {
            return joinPoint.proceed(); // 让方法级别的切面处理
        }
        
        doCheck(requireLogin, joinPoint);
        return joinPoint.proceed();
    }

    /**
     * 执行登录检查
     * 
     * @param requireLogin 登录注解
     * @param joinPoint 切入点
     * @throws BusinessException 未登录或无权限时抛出异常
     */
    private void doCheck(RequireLogin requireLogin, ProceedingJoinPoint joinPoint) {
        UserContext context = UserContextHolder.getContext();
        
        // 检查是否登录
        if (requireLogin.required() && context == null) {
            log.warn("未登录访问受保护接口: {}", joinPoint.getSignature().toShortString());
            throw new BusinessException(401, "请先登录");
        }
        
        // 检查用户类型
        String[] allowedTypes = requireLogin.allowedTypes();
        if (context != null && allowedTypes.length > 0) {
            boolean allowed = Arrays.asList(allowedTypes).contains(context.getUserType());
            if (!allowed) {
                log.warn("用户类型不匹配, userId: {}, userType: {}, allowedTypes: {}", 
                        context.getUserId(), context.getUserType(), Arrays.toString(allowedTypes));
                throw new BusinessException(403, "无权访问该接口");
            }
        }
    }
}
