package xyh.dp.mall.common.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户上下文信息
 * 存储当前登录用户的基本信息，从JWT Token中解析
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（数据库主键）
     */
    private Long userId;

    /**
     * 用户类型: FARMER-农户, SUPPLIER-供销商
     */
    private String userType;

    /**
     * 业务用户ID（如：FARMER001, SUPPLIER001）
     * 用于供给匹配等业务场景
     */
    private String businessUserId;

    /**
     * 判断是否为农户
     * 
     * @return true-农户, false-非农户
     */
    public boolean isFarmer() {
        return "FARMER".equals(userType);
    }

    /**
     * 判断是否为供销商
     * 
     * @return true-供销商, false-非供销商
     */
    public boolean isSupplier() {
        return "SUPPLIER".equals(userType);
    }

    /**
     * 获取业务用户ID（带前缀）
     * 如果businessUserId为空，则根据userType和userId自动生成
     * 
     * @return 业务用户ID
     */
    public String getBusinessUserId() {
        if (businessUserId != null && !businessUserId.isEmpty()) {
            return businessUserId;
        }
        // 自动生成业务ID
        if (isFarmer()) {
            return "FARMER" + String.format("%03d", userId);
        } else if (isSupplier()) {
            return "SUPPLY" + String.format("%03d", userId);
        }
        return "USER" + String.format("%03d", userId);
    }
}
