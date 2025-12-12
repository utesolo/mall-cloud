package xyh.dp.mall.common.context;

/**
 * 用户上下文持有者
 * 使用ThreadLocal存储当前线程的用户信息
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前用户上下文
     * 
     * @param userContext 用户上下文
     */
    public static void setContext(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    /**
     * 获取当前用户上下文
     * 
     * @return 用户上下文，如果未登录返回null
     */
    public static UserContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未登录返回null
     */
    public static Long getUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户类型
     * 
     * @return 用户类型，如果未登录返回null
     */
    public static String getUserType() {
        UserContext context = getContext();
        return context != null ? context.getUserType() : null;
    }

    /**
     * 获取当前业务用户ID（如：FARMER001）
     * 
     * @return 业务用户ID，如果未登录返回null
     */
    public static String getBusinessUserId() {
        UserContext context = getContext();
        return context != null ? context.getBusinessUserId() : null;
    }

    /**
     * 判断当前用户是否为农户
     * 
     * @return true-农户, false-非农户或未登录
     */
    public static boolean isFarmer() {
        UserContext context = getContext();
        return context != null && context.isFarmer();
    }

    /**
     * 判断当前用户是否为供销商
     * 
     * @return true-供销商, false-非供销商或未登录
     */
    public static boolean isSupplier() {
        UserContext context = getContext();
        return context != null && context.isSupplier();
    }

    /**
     * 清除当前用户上下文
     * 必须在请求结束后调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
