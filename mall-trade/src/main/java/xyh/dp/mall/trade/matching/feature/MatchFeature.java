package xyh.dp.mall.trade.matching.feature;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 匹配特征类
 * 从种植计划和商品中提取的6维特征向量
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "匹配特征向量")
public class MatchFeature implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 关联ID ====================

    @Schema(description = "种植计划ID")
    private String planId;

    @Schema(description = "商品ID")
    private Long productId;

    // ==================== 6维特征 ====================

    /**
     * 特征1: 品种一致性 (0-100)
     * 计算种植计划的品种与商品品种的相似度
     */
    @Schema(description = "品种一致性得分(0-100)")
    private BigDecimal varietyScore;

    /**
     * 特征2: 区域适配 (0-100)
     * 计算种植区域是否在商品适配区域内
     */
    @Schema(description = "区域适配得分(0-100)")
    private BigDecimal regionScore;

    /**
     * 特征3: 气候匹配 (0-100)
     * 根据区域气候与商品种植环境要求匹配
     */
    @Schema(description = "气候匹配得分(0-100)")
    private BigDecimal climateScore;

    /**
     * 特征4: 种植时间与季节匹配 (0-100)
     * 计算种植时间是否在商品适宜的种植季节
     */
    @Schema(description = "季节匹配得分(0-100)")
    private BigDecimal seasonScore;

    /**
     * 特征5: 种子质量 (0-100)
     * 综合发芽率、纯度、种植难度等指标
     */
    @Schema(description = "种子质量得分(0-100)")
    private BigDecimal qualityScore;

    /**
     * 特征6: 供需意图匹配 (0-100)
     * 目标用途与商品描述的匹配度
     */
    @Schema(description = "供需意图匹配得分(0-100)")
    private BigDecimal intentScore;

    // ==================== 综合得分 ====================

    /**
     * 加权综合得分 (0-100)
     */
    @Schema(description = "加权综合得分(0-100)")
    private BigDecimal totalScore;

    /**
     * 匹配等级: A-优秀(>=80), B-良好(>=60), C-一般(>=40), D-较差(<40)
     */
    @Schema(description = "匹配等级: A-优秀, B-良好, C-一般, D-较差")
    private String matchGrade;

    /**
     * 匹配建议描述
     */
    @Schema(description = "匹配建议描述")
    private String recommendation;

    /**
     * 根据总分计算匹配等级
     * 
     * @return 匹配等级
     */
    public String calculateGrade() {
        if (totalScore == null) {
            return "D";
        }
        int score = totalScore.intValue();
        if (score >= 80) {
            return "A";
        } else if (score >= 60) {
            return "B";
        } else if (score >= 40) {
            return "C";
        } else {
            return "D";
        }
    }

    /**
     * 转换为CSV行格式（用于导出训练数据）
     * 
     * @return CSV行字符串
     */
    public String toCsvLine() {
        return String.format("%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s",
                planId,
                productId,
                varietyScore != null ? varietyScore : BigDecimal.ZERO,
                regionScore != null ? regionScore : BigDecimal.ZERO,
                climateScore != null ? climateScore : BigDecimal.ZERO,
                seasonScore != null ? seasonScore : BigDecimal.ZERO,
                qualityScore != null ? qualityScore : BigDecimal.ZERO,
                intentScore != null ? intentScore : BigDecimal.ZERO,
                totalScore != null ? totalScore : BigDecimal.ZERO,
                matchGrade != null ? matchGrade : "D");
    }

    /**
     * CSV表头
     * 
     * @return CSV表头字符串
     */
    public static String csvHeader() {
        return "plan_id,product_id,variety_score,region_score,climate_score,season_score,quality_score,intent_score,total_score,match_grade";
    }
}
