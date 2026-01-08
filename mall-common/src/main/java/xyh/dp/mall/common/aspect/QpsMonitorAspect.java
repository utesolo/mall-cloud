package xyh.dp.mall.common.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import xyh.dp.mall.common.monitor.QpsMonitor;

import jakarta.servlet.http.HttpServletRequest;

/**
 * QPS监控切面
 * 自动拦截Controller请求并统计QPS
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QpsMonitorAspect {

    private final QpsMonitor qpsMonitor;

    /**
     * 切点：拦截所有Controller方法
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
              "@within(org.springframework.stereotype.Controller)")
    public void controllerPointcut() {}

    /**
     * 环绕通知：记录QPS
     * 
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("controllerPointcut()")
    public Object monitorQps(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求路径
        String endpoint = getRequestPath();
        
        // 记录请求（QPS统计）
        if (endpoint != null) {
            qpsMonitor.recordRequest(endpoint);
        }
        
        // 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 获取请求路径
     * 
     * @return 请求路径（如：/product/page）
     */
    private String getRequestPath() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getRequestURI();
            }
        } catch (Exception e) {
            log.debug("获取请求路径失败", e);
        }
        return null;
    }
}
