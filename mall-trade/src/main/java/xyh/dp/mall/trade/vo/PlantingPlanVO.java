package xyh.dp.mall.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 种植计划VO（供给匹配）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "种植计划信息")
public class PlantingPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "种植计划ID", example = "PLAN20231001")
    private String planId;

    @Schema(description = "农户ID", example = "FARMER001")
    private String farmerId;

    @Schema(description = "种植面积(亩)", example = "30.0")
    private BigDecimal plantingArea;

    @Schema(description = "种植品种", example = "鹤首葫芦")
    private String variety;

    @Schema(description = "预计产量(个)", example = "10000")
    private Integer expectedYield;

    @Schema(description = "种植时间", example = "2023-04-10")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plantingDate;

    @Schema(description = "目标用途", example = "工艺品制作")
    private String targetUsage;

    @Schema(description = "种植计划摘要", example = "山东菏泽·鹤首葫芦·30亩·预计产量10000个·用于工艺品制作")
    private String planSummary;

    @Schema(description = "种植区域", example = "山东菏泽")
    private String region;

    @Schema(description = "供需匹配度(0-100)", example = "85")
    private Integer matchScore;

    @Schema(description = "供销商ID", example = "SUPPLY001")
    private String supplierId;

    @Schema(description = "匹配时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchTime;

    @Schema(description = "区域气候匹配描述", example = "当前山东地区适合鹤首葫芦种植")
    private String climateMatch;

    @Schema(description = "匹配状态: PENDING-待匹配, MATCHED-已匹配, CONFIRMED-已确认, CANCELLED-已取消")
    private String matchStatus;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
