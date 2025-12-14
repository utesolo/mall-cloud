package xyh.dp.mall.trade.tracking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为埋点事件实体
 * 用于收集用户点击/确认行为作为正样本
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_tracking_event")
public class UserTrackingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 事件ID（业务唯一标识）
     */
    private String eventId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户类型: FARMER-农户, SUPPLIER-供销商
     */
    private String userType;

    /**
     * 事件类型
     * PRODUCT_VIEW - 查看商品
     * PRODUCT_CLICK - 点击商品
     * MATCH_VIEW - 查看匹配结果
     * MATCH_CLICK - 点击匹配商品
     * MATCH_CONFIRM - 确认匹配
     * ORDER_CREATE - 创建订单
     */
    private String eventType;

    // ==================== 关联ID ====================

    /**
     * 种植计划ID
     */
    private String planId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 供销商ID
     */
    private String supplierId;

    // ==================== 匹配特征快照（用于训练） ====================

    /**
     * 品种一致性得分
     */
    private BigDecimal varietyScore;

    /**
     * 区域适配得分
     */
    private BigDecimal regionScore;

    /**
     * 气候匹配得分
     */
    private BigDecimal climateScore;

    /**
     * 季节匹配得分
     */
    private BigDecimal seasonScore;

    /**
     * 种子质量得分
     */
    private BigDecimal qualityScore;

    /**
     * 供需意图得分
     */
    private BigDecimal intentScore;

    /**
     * 综合得分
     */
    private BigDecimal totalScore;

    /**
     * 匹配等级
     */
    private String matchGrade;

    // ==================== 上下文信息 ====================

    /**
     * 用户设备类型: PC, MOBILE, TABLET
     */
    private String deviceType;

    /**
     * 用户来源渠道
     */
    private String channel;

    /**
     * 页面停留时长(秒)
     */
    private Integer stayDuration;

    /**
     * 是否为正样本（1=用户确认/购买，0=仅浏览）
     */
    private Integer isPositive;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 扩展数据（JSON格式，存储其他上下文信息）
     */
    private String extData;

    /**
     * 转换为CSV行格式（用于导出训练数据）
     * 
     * @return CSV行字符串
     */
    public String toCsvLine() {
        return String.format("%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%d",
                planId != null ? planId : "",
                productId != null ? productId : 0L,
                varietyScore != null ? varietyScore : BigDecimal.ZERO,
                regionScore != null ? regionScore : BigDecimal.ZERO,
                climateScore != null ? climateScore : BigDecimal.ZERO,
                seasonScore != null ? seasonScore : BigDecimal.ZERO,
                qualityScore != null ? qualityScore : BigDecimal.ZERO,
                intentScore != null ? intentScore : BigDecimal.ZERO,
                totalScore != null ? totalScore : BigDecimal.ZERO,
                matchGrade != null ? matchGrade : "D",
                isPositive != null ? isPositive : 0);
    }

    /**
     * CSV表头（用于训练数据导出）
     * 
     * @return CSV表头字符串
     */
    public static String csvHeader() {
        return "plan_id,product_id,variety_score,region_score,climate_score,season_score,quality_score,intent_score,total_score,match_grade,is_positive";
    }
}
