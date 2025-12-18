package xyh.dp.mall.trade.matching.async;

import lombok.Data;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 匹配结果实体
 * 包含种植计划的Top-N最佳匹配商品
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class MatchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认返回的最佳结果数量
     */
    public static final int DEFAULT_TOP_N = 5;

    /**
     * 种植计划ID
     */
    private String planId;

    /**
     * 已评估商品总数
     */
    private Integer totalEvaluated;

    /**
     * Top-N匹配项（按得分降序）
     */
    private List<MatchResultItem> topMatches;

    /**
     * 最佳匹配（topMatches中的第一项）
     */
    private MatchResultItem bestMatch;

    /**
     * 匹配完成时间
     */
    private LocalDateTime matchTime;

    /**
     * 匹配耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 创建空结果
     *
     * @param planId 种植计划ID
     * @return 空的MatchResult
     */
    public static MatchResult empty(String planId) {
        MatchResult result = new MatchResult();
        result.setPlanId(planId);
        result.setTotalEvaluated(0);
        result.setTopMatches(new ArrayList<>());
        result.setMatchTime(LocalDateTime.now());
        return result;
    }

    /**
     * 从匹配特征创建结果
     *
     * @param planId         种植计划ID
     * @param features       匹配特征列表
     * @param topN           保留的最佳结果数量
     * @param totalEvaluated 已评估商品总数
     * @param durationMs     匹配耗时（毫秒）
     * @return 包含Top-N项的MatchResult
     */
    public static MatchResult fromFeatures(String planId, List<MatchFeature> features,
                                            int topN, int totalEvaluated, long durationMs) {
        MatchResult result = new MatchResult();
        result.setPlanId(planId);
        result.setTotalEvaluated(totalEvaluated);
        result.setMatchTime(LocalDateTime.now());
        result.setDurationMs(durationMs);

        // Sort by score descending and take top-N
        List<MatchResultItem> items = features.stream()
                .sorted(Comparator.comparing(MatchFeature::getTotalScore).reversed())
                .limit(topN)
                .map(MatchResultItem::fromFeature)
                .toList();

        result.setTopMatches(new ArrayList<>(items));

        // Set best match
        if (!items.isEmpty()) {
            result.setBestMatch(items.get(0));
        }

        return result;
    }

    /**
     * 检查是否有匹配结果
     *
     * @return 有匹配结果返回true
     */
    public boolean hasMatches() {
        return topMatches != null && !topMatches.isEmpty();
    }

    /**
     * 获取匹配数量
     *
     * @return 匹配数量
     */
    public int getMatchCount() {
        return topMatches != null ? topMatches.size() : 0;
    }

    /**
     * 单个匹配结果项
     */
    @Data
    public static class MatchResultItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 排名位置（1开始）
         */
        private Integer rank;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 供应商ID
         */
        private Long supplierId;

        /**
         * 匹配总分 (0-100)
         */
        private BigDecimal totalScore;

        /**
         * 匹配等级 (A/B/C/D)
         */
        private String matchGrade;

        /**
         * 各维度得分
         */
        private BigDecimal varietyScore;
        private BigDecimal regionScore;
        private BigDecimal climateScore;
        private BigDecimal seasonScore;
        private BigDecimal qualityScore;
        private BigDecimal intentScore;

        /**
         * 匹配建议文本
         */
        private String recommendation;

        /**
         * 商品价格
         */
        private BigDecimal price;

        /**
         * 商品库存
         */
        private Integer stock;

        /**
         * 从MatchFeature创建
         *
         * @param feature 匹配特征
         * @return MatchResultItem
         */
        public static MatchResultItem fromFeature(MatchFeature feature) {
            MatchResultItem item = new MatchResultItem();
            item.setProductId(feature.getProductId());
            item.setProductName(feature.getProductName());
            item.setSupplierId(feature.getSupplierId());
            item.setTotalScore(feature.getTotalScore());
            item.setMatchGrade(feature.getMatchGrade());
            item.setVarietyScore(feature.getVarietyScore());
            item.setRegionScore(feature.getRegionScore());
            item.setClimateScore(feature.getClimateScore());
            item.setSeasonScore(feature.getSeasonScore());
            item.setQualityScore(feature.getQualityScore());
            item.setIntentScore(feature.getIntentScore());
            item.setRecommendation(feature.getRecommendation());
            item.setPrice(feature.getPrice());
            item.setStock(feature.getStock());
            return item;
        }
    }
}
