package xyh.dp.mall.trade.matching.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.feature.FeatureWeight;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 匹配评分计算器
 * 规则引擎 + 加权评分实现MVP版本
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchScoreCalculator {

    private static final BigDecimal HARD_MISMATCH_THRESHOLD = new BigDecimal("40");

    private final MatchFeatureExtractor featureExtractor;
    private final FeatureWeight featureWeight;

    /**
     * 计算单个商品的匹配得分
     * 
     * @param plan 种植计划
     * @param product 商品信息
     * @return 完整的匹配特征（包含加权总分）
     */
    public MatchFeature calculateScore(PlantingPlan plan, ProductDTO product) {
        // 提取特征
        MatchFeature feature = featureExtractor.extractFeatures(plan, product);

        // 硬性约束过滤：品种、区域或季节任一严重不匹配则直接判为0分
        if (isHardMismatch(feature)) {
            feature.setTotalScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            feature.setMatchGrade("D");
            feature.setRecommendation("品种/区域/季节存在硬性不匹配，不推荐该商品。");

            log.info("硬性不匹配: planId={}, productId={}, varietyScore={}, regionScore={}, seasonScore={}",
                    plan.getPlanId(), product.getId(),
                    feature.getVarietyScore(), feature.getRegionScore(), feature.getSeasonScore());
            return feature;
        }
        
        // 获取归一化权重
        FeatureWeight weight = featureWeight.normalize();
        
        // 计算加权总分
        BigDecimal totalScore = BigDecimal.ZERO;
        totalScore = totalScore.add(feature.getVarietyScore().multiply(weight.getVariety()));
        totalScore = totalScore.add(feature.getRegionScore().multiply(weight.getRegion()));
        totalScore = totalScore.add(feature.getClimateScore().multiply(weight.getClimate()));
        totalScore = totalScore.add(feature.getSeasonScore().multiply(weight.getSeason()));
        totalScore = totalScore.add(feature.getQualityScore().multiply(weight.getQuality()));
        totalScore = totalScore.add(feature.getIntentScore().multiply(weight.getIntent()));
        
        // 设置总分和等级
        feature.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        feature.setMatchGrade(feature.calculateGrade());
        feature.setRecommendation(generateRecommendation(feature));
        
        log.info("计算匹配得分: planId={}, productId={}, totalScore={}, grade={}", 
                plan.getPlanId(), product.getId(), feature.getTotalScore(), feature.getMatchGrade());
        
        return feature;
    }

    /**
     * 判断是否存在硬性不匹配
     * 品种、区域或季节任一维度得分低于阈值(40)则视为不匹配
     *
     * @param feature 匹配特征
     * @return 存在硬性不匹配返回true
     */
    public boolean isHardMismatch(MatchFeature feature) {
        if (feature == null) {
            return true;
        }
        BigDecimal variety = feature.getVarietyScore();
        BigDecimal region = feature.getRegionScore();
        BigDecimal season = feature.getSeasonScore();

        if (variety == null || region == null || season == null) {
            return false;
        }

        return variety.compareTo(HARD_MISMATCH_THRESHOLD) < 0
                || region.compareTo(HARD_MISMATCH_THRESHOLD) < 0
                || season.compareTo(HARD_MISMATCH_THRESHOLD) < 0;
    }

    /**
     * 批量计算匹配得分并排序
     * 
     * @param plan 种植计划
     * @param products 商品列表
     * @return 按匹配度排序的匹配特征列表
     */
    public List<MatchFeature> calculateAndRankScores(PlantingPlan plan, List<ProductDTO> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        
        return products.stream()
                .map(product -> calculateScore(plan, product))
                .sorted(Comparator.comparing(MatchFeature::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取最佳匹配
     * 
     * @param plan 种植计划
     * @param products 商品列表
     * @return 最佳匹配特征
     */
    public MatchFeature findBestMatch(PlantingPlan plan, List<ProductDTO> products) {
        List<MatchFeature> rankedFeatures = calculateAndRankScores(plan, products);
        return rankedFeatures.isEmpty() ? null : rankedFeatures.get(0);
    }

    /**
     * 获取推荐商品列表（得分>=60的商品）
     * 
     * @param plan 种植计划
     * @param products 商品列表
     * @param limit 返回数量限制
     * @return 推荐的匹配特征列表
     */
    public List<MatchFeature> getRecommendations(PlantingPlan plan, List<ProductDTO> products, int limit) {
        return calculateAndRankScores(plan, products).stream()
                .filter(f -> f.getTotalScore().compareTo(new BigDecimal("60")) >= 0)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 生成匹配建议描述
     * 
     * @param feature 匹配特征
     * @return 建议描述
     */
    private String generateRecommendation(MatchFeature feature) {
        StringBuilder sb = new StringBuilder();
        
        // 根据各维度得分生成建议
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        
        if (feature.getVarietyScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("品种高度匹配");
        } else if (feature.getVarietyScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("品种匹配度较低");
        }
        
        if (feature.getRegionScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("区域适配性好");
        } else if (feature.getRegionScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("区域适配性不足");
        }
        
        if (feature.getClimateScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("气候条件适宜");
        } else if (feature.getClimateScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("气候条件可能不适宜");
        }
        
        if (feature.getSeasonScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("种植季节适合");
        } else if (feature.getSeasonScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("种植季节可能不匹配");
        }
        
        if (feature.getQualityScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("种子质量优良");
        } else if (feature.getQualityScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("种子质量一般");
        }
        
        if (feature.getIntentScore().compareTo(new BigDecimal("80")) >= 0) {
            strengths.add("用途匹配度高");
        } else if (feature.getIntentScore().compareTo(new BigDecimal("50")) < 0) {
            weaknesses.add("用途匹配度较低");
        }
        
        // 组装建议
        if (!strengths.isEmpty()) {
            sb.append("优势：").append(String.join("、", strengths)).append("。");
        }
        if (!weaknesses.isEmpty()) {
            sb.append("注意：").append(String.join("、", weaknesses)).append("。");
        }
        
        // 总体评价
        switch (feature.getMatchGrade()) {
            case "A":
                sb.append("整体匹配度优秀，强烈推荐。");
                break;
            case "B":
                sb.append("整体匹配度良好，建议选用。");
                break;
            case "C":
                sb.append("整体匹配度一般，可作为备选。");
                break;
            default:
                sb.append("整体匹配度较低，建议寻找更合适的商品。");
        }
        
        return sb.toString();
    }
}
