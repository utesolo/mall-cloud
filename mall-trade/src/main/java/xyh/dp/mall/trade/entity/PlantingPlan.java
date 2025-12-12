package xyh.dp.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 种植计划实体（供给匹配）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("planting_plan")
public class PlantingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息 ====================

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 种植计划ID（业务唯一标识，如：PLAN20231001）
     */
    private String planId;

    /**
     * 农户ID（农户唯一标识，如：FARMER001）
     */
    private String farmerId;

    /**
     * 种植面积(亩)
     */
    private BigDecimal plantingArea;

    /**
     * 种植品种（如：鹤首葫芦）
     */
    private String variety;

    /**
     * 预计产量(个)
     */
    private Integer expectedYield;

    /**
     * 种植时间
     */
    private LocalDate plantingDate;

    /**
     * 目标用途（如：工艺品制作、食用、观赏等）
     */
    private String targetUsage;

    /**
     * 种植计划摘要（自动生成，如："山东菏泽·鹤首葫芦·30亩·预计产量10000个·用于工艺品制作"）
     */
    private String planSummary;

    /**
     * 种植区域（如：山东菏泽）
     */
    private String region;

    // ==================== 供需匹配信息 ====================

    /**
     * 供需匹配度(0-100)，算法计算
     */
    private Integer matchScore;

    /**
     * 供销商ID（匹配的供销商，如：SUPPLY001）
     */
    private String supplierId;

    /**
     * 匹配时间
     */
    private LocalDateTime matchTime;

    /**
     * 区域气候匹配描述（如："当前山东地区适合鹤首葫芦种植"）
     */
    private String climateMatch;

    /**
     * 匹配状态: PENDING-待匹配, MATCHED-已匹配, CONFIRMED-已确认, CANCELLED-已取消
     */
    private String matchStatus;

    // ==================== 时间戳 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
