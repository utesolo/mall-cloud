package xyh.dp.mall.trade.tracking.aspect;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.trade.tracking.annotation.TrackEvent;
import xyh.dp.mall.trade.tracking.dto.TrackingEventDTO;
import xyh.dp.mall.trade.tracking.service.TrackingService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 埋点AOP切面
 * 自动拦截标记了@TrackEvent的方法并记录埋点
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(100)  // 在事务之后执行
@Component
@RequiredArgsConstructor
public class TrackingAspect {

    private final TrackingService trackingService;

    /**
     * 定义切点：所有标记了@TrackEvent的方法
     */
    @Pointcut("@annotation(xyh.dp.mall.trade.tracking.annotation.TrackEvent)")
    public void trackEventPointcut() {
    }

    /**
     * 环绕通知：记录埋点
     * 
     * @param joinPoint 切点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("trackEventPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        TrackEvent trackEvent = method.getAnnotation(TrackEvent.class);

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                // 异步记录埋点，不影响主流程
                recordTrackEvent(joinPoint, trackEvent, result, success, 
                        System.currentTimeMillis() - startTime, errorMsg);
            } catch (Exception e) {
                log.error("埋点记录异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录埋点事件
     * 
     * @param joinPoint 切点
     * @param trackEvent 埋点注解
     * @param result 方法返回值
     * @param success 是否成功
     * @param duration 执行时长
     * @param errorMsg 错误信息
     */
    private void recordTrackEvent(ProceedingJoinPoint joinPoint, TrackEvent trackEvent, 
                                   Object result, boolean success, long duration, String errorMsg) {
        // 构建扩展数据
        Map<String, Object> extData = new HashMap<>();
        extData.put("method", joinPoint.getSignature().toShortString());
        extData.put("success", success);
        extData.put("duration", duration);
        
        if (!success && errorMsg != null) {
            extData.put("error", errorMsg);
        }

        // 记录方法参数
        if (trackEvent.recordParams()) {
            Map<String, Object> params = extractParams(joinPoint);
            extData.put("params", params);
        }

        // 记录返回值
        if (trackEvent.recordResult() && result != null && success) {
            try {
                extData.put("result", result.getClass().getSimpleName());
            } catch (Exception e) {
                log.debug("序列化返回值失败: {}", e.getMessage());
            }
        }

        // 从参数中提取planId和productId
        String planId = extractPlanId(joinPoint);
        Long productId = extractProductId(joinPoint);

        // 创建埋点DTO
        TrackingEventDTO dto = new TrackingEventDTO();
        dto.setEventType(trackEvent.eventType());
        dto.setPlanId(planId);
        dto.setProductId(productId);
        dto.setChannel("api");
        dto.setExtData(JSON.toJSONString(extData));

        // 异步记录
        trackingService.trackEvent(dto);
    }

    /**
     * 提取方法参数
     * 
     * @param joinPoint 切点
     * @return 参数映射
     */
    private Map<String, Object> extractParams(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            String paramName = parameters[i].getName();
            Object paramValue = args[i];
            
            // 跳过敏感或过大的对象
            if (paramValue != null && !isSensitive(paramName)) {
                if (isPrimitive(paramValue)) {
                    params.put(paramName, paramValue);
                } else {
                    params.put(paramName, paramValue.getClass().getSimpleName());
                }
            }
        }
        return params;
    }

    /**
     * 从参数中提取planId
     * 
     * @param joinPoint 切点
     * @return planId
     */
    private String extractPlanId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            if ("planId".equals(paramNames[i]) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }

    /**
     * 从参数中提取productId
     * 
     * @param joinPoint 切点
     * @return productId
     */
    private Long extractProductId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            if ("productId".equals(paramNames[i]) || "id".equals(paramNames[i])) {
                if (args[i] instanceof Long) {
                    return (Long) args[i];
                } else if (args[i] instanceof Integer) {
                    return ((Integer) args[i]).longValue();
                }
            }
        }
        return null;
    }

    /**
     * 检查参数名是否敏感
     * 
     * @param paramName 参数名
     * @return 是否敏感
     */
    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") || lower.contains("token") || 
               lower.contains("secret") || lower.contains("key");
    }

    /**
     * 检查是否为基本类型
     * 
     * @param obj 对象
     * @return 是否为基本类型
     */
    private boolean isPrimitive(Object obj) {
        return obj instanceof String || obj instanceof Number || 
               obj instanceof Boolean || obj instanceof Character;
    }
}
