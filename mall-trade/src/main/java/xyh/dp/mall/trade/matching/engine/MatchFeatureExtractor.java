package xyh.dp.mall.trade.matching.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.feature.MatchFeature;
import xyh.dp.mall.trade.matching.feature.RegionClimateData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

/**
 * 匹配特征提取器
 * 从种植计划和商品中提取6维特征向量
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchFeatureExtractor {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * 提取匹配特征
     * 
     * @param plan 种植计划
     * @param product 商品信息
     * @return 匹配特征向量
     */
    public MatchFeature extractFeatures(PlantingPlan plan, ProductDTO product) {
        log.debug("提取匹配特征: planId={}, productId={}", plan.getPlanId(), product.getId());
        
        return MatchFeature.builder()
                .planId(plan.getPlanId())
                .productId(product.getId())
                .productName(product.getName())
                .supplierId(product.getSupplierId())
                .price(product.getPrice())
                .stock(product.getStock())
                .varietyScore(calculateVarietyScore(plan.getVariety(), product.getVariety()))
                .regionScore(calculateRegionScore(plan.getRegion(), product.getRegions()))
                .climateScore(calculateClimateScore(plan.getRegion(), product))
                .seasonScore(calculateSeasonScore(plan.getPlantingDate(), product.getPlantingSeasons()))
                .qualityScore(calculateQualityScore(product))
                .intentScore(calculateIntentScore(plan.getTargetUsage(), product.getDescription()))
                .build();
    }

    /**
     * 特征1: 品种一致性得分
     * 使用编辑距离算法计算相似度
     * 
     * @param planVariety 种植计划品种
     * @param productVariety 商品品种
     * @return 得分(0-100)
     */
    private BigDecimal calculateVarietyScore(String planVariety, String productVariety) {
        if (!StringUtils.hasText(planVariety) || !StringUtils.hasText(productVariety)) {
            return ZERO;
        }

        // 完全匹配
        if (planVariety.equals(productVariety)) {
            return HUNDRED;
        }

        // 包含匹配
        if (planVariety.contains(productVariety) || productVariety.contains(planVariety)) {
            return new BigDecimal("85");
        }

        // 相似度计算（基于编辑距离）
        double similarity = calculateStringSimilarity(planVariety, productVariety);
        return new BigDecimal(similarity * 100).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 特征2: 区域适配得分
     * 
     * @param planRegion 种植区域
     * @param productRegions 商品适配区域列表
     * @return 得分(0-100)
     */
    private BigDecimal calculateRegionScore(String planRegion, List<String> productRegions) {
        if (!StringUtils.hasText(planRegion) || productRegions == null || productRegions.isEmpty()) {
            return new BigDecimal("50"); // 无区域信息时给默认分
        }

        // 精确匹配
        for (String region : productRegions) {
            if (planRegion.contains(region) || region.contains(planRegion)) {
                return HUNDRED;
            }
        }

        // 大区匹配（如"山东菏泽"匹配"华东"）
        for (String region : productRegions) {
            if (RegionClimateData.belongsToMajorRegion(planRegion, region)) {
                return new BigDecimal("80");
            }
        }

        // 无匹配
        return new BigDecimal("30");
    }

    /**
     * 特征3: 气候匹配得分
     * 根据区域气候与商品种植环境要求匹配
     * 
     * @param planRegion 种植区域
     * @param product 商品信息
     * @return 得分(0-100)
     */
    private BigDecimal calculateClimateScore(String planRegion, ProductDTO product) {
        RegionClimateData climate = RegionClimateData.getByRegion(planRegion);
        
        BigDecimal score = new BigDecimal("100");
        int penaltyCount = 0;

        // 温度匹配
        if (product.getMinTemperature() != null && product.getMaxTemperature() != null) {
            BigDecimal regionTemp = climate.getAvgTemperature();
            if (regionTemp.compareTo(product.getMinTemperature()) < 0 || 
                regionTemp.compareTo(product.getMaxTemperature()) > 0) {
                // 超出范围，计算偏离程度
                BigDecimal deviation;
                if (regionTemp.compareTo(product.getMinTemperature()) < 0) {
                    deviation = product.getMinTemperature().subtract(regionTemp);
                } else {
                    deviation = regionTemp.subtract(product.getMaxTemperature());
                }
                // 每偏离1度扣5分，最多扣30分
                BigDecimal penalty = deviation.multiply(new BigDecimal("5")).min(new BigDecimal("30"));
                score = score.subtract(penalty);
                penaltyCount++;
            }
        }

        // 湿度匹配
        if (product.getMinHumidity() != null && product.getMaxHumidity() != null) {
            BigDecimal regionHumidity = climate.getAvgHumidity();
            if (regionHumidity.compareTo(product.getMinHumidity()) < 0 || 
                regionHumidity.compareTo(product.getMaxHumidity()) > 0) {
                score = score.subtract(new BigDecimal("15"));
                penaltyCount++;
            }
        }

        // 光照匹配
        if (StringUtils.hasText(product.getLightRequirement()) && 
            StringUtils.hasText(climate.getLightCondition())) {
            if (!product.getLightRequirement().equals(climate.getLightCondition())) {
                // 不完全匹配，但不完全不兼容
                if (!"SHADE".equals(climate.getLightCondition()) || 
                    !"FULL_SUN".equals(product.getLightRequirement())) {
                    score = score.subtract(new BigDecimal("10"));
                } else {
                    score = score.subtract(new BigDecimal("25"));
                }
                penaltyCount++;
            }
        }

        return score.max(ZERO);
    }

    /**
     * 特征4: 种植时间与季节匹配得分
     * 
     * @param plantingDate 种植时间
     * @param plantingSeasons 适宜种植季节
     * @return 得分(0-100)
     */
    private BigDecimal calculateSeasonScore(LocalDate plantingDate, List<String> plantingSeasons) {
        if (plantingDate == null) {
            return new BigDecimal("50");
        }

        String currentSeason = getSeasonFromDate(plantingDate);
        
        if (plantingSeasons == null || plantingSeasons.isEmpty()) {
            return new BigDecimal("60"); // 无季节信息时给默认分
        }

        // 精确季节匹配
        for (String season : plantingSeasons) {
            if (currentSeason.equals(season)) {
                return HUNDRED;
            }
        }

        // 相邻季节匹配
        String[] seasonOrder = {"春季", "夏季", "秋季", "冬季"};
        int currentIndex = Arrays.asList(seasonOrder).indexOf(currentSeason);
        for (String season : plantingSeasons) {
            int seasonIndex = Arrays.asList(seasonOrder).indexOf(season);
            if (Math.abs(currentIndex - seasonIndex) == 1 || 
                (currentIndex == 0 && seasonIndex == 3) || 
                (currentIndex == 3 && seasonIndex == 0)) {
                return new BigDecimal("70");
            }
        }

        return new BigDecimal("40");
    }

    /**
     * 特征5: 种子质量得分
     * 综合发芽率、纯度、种植难度等指标
     * 
     * @param product 商品信息
     * @return 得分(0-100)
     */
    private BigDecimal calculateQualityScore(ProductDTO product) {
        BigDecimal score = ZERO;
        int factorCount = 0;

        // 发芽率（权重40%）
        if (product.getGerminationRate() != null) {
            score = score.add(product.getGerminationRate().multiply(new BigDecimal("0.4")));
            factorCount++;
        }

        // 品种纯度（权重30%）
        if (product.getPurity() != null) {
            score = score.add(product.getPurity().multiply(new BigDecimal("0.3")));
            factorCount++;
        }

        // 种植难度（权重30%）
        if (StringUtils.hasText(product.getDifficulty())) {
            BigDecimal difficultyScore;
            switch (product.getDifficulty()) {
                case "EASY":
                    difficultyScore = HUNDRED;
                    break;
                case "MEDIUM":
                    difficultyScore = new BigDecimal("70");
                    break;
                case "HARD":
                    difficultyScore = new BigDecimal("50");
                    break;
                default:
                    difficultyScore = new BigDecimal("60");
            }
            score = score.add(difficultyScore.multiply(new BigDecimal("0.3")));
            factorCount++;
        }

        // 如果没有任何质量指标，返回默认分
        if (factorCount == 0) {
            return new BigDecimal("60");
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 特征6: 供需意图匹配得分
     * 基于目标用途与商品描述的关键词匹配
     * 
     * @param targetUsage 目标用途
     * @param description 商品描述
     * @return 得分(0-100)
     */
    private BigDecimal calculateIntentScore(String targetUsage, String description) {
        if (!StringUtils.hasText(targetUsage)) {
            return new BigDecimal("50");
        }

        // 用途关键词映射
        String[][] usageKeywords = {
            {"工艺品制作", "工艺", "手工", "艺术", "装饰", "观赏"},
            {"食用", "食用加工", "鲜食", "餐饮", "美食", "营养"},
            {"鲜食销售", "新鲜", "鲜果", "采摘", "直销"},
            {"粮食储备", "粮食", "储存", "加工", "饲料"},
            {"观赏", "园艺", "绿化", "盆栽", "庭院"}
        };

        // 查找匹配的用途类别
        String[] matchedKeywords = null;
        for (String[] keywords : usageKeywords) {
            for (String keyword : keywords) {
                if (targetUsage.contains(keyword)) {
                    matchedKeywords = keywords;
                    break;
                }
            }
            if (matchedKeywords != null) break;
        }

        if (matchedKeywords == null) {
            return new BigDecimal("50");
        }

        // 检查商品描述中是否包含匹配的关键词
        if (!StringUtils.hasText(description)) {
            return new BigDecimal("60");
        }

        int matchCount = 0;
        for (String keyword : matchedKeywords) {
            if (description.contains(keyword)) {
                matchCount++;
            }
        }

        if (matchCount >= 2) {
            return HUNDRED;
        } else if (matchCount == 1) {
            return new BigDecimal("80");
        } else {
            return new BigDecimal("50");
        }
    }

    /**
     * 根据日期获取季节
     * 
     * @param date 日期
     * @return 季节名称
     */
    private String getSeasonFromDate(LocalDate date) {
        Month month = date.getMonth();
        switch (month) {
            case MARCH:
            case APRIL:
            case MAY:
                return "春季";
            case JUNE:
            case JULY:
            case AUGUST:
                return "夏季";
            case SEPTEMBER:
            case OCTOBER:
            case NOVEMBER:
                return "秋季";
            default:
                return "冬季";
        }
    }

    /**
     * 计算字符串相似度（基于Levenshtein距离）
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度(0-1)
     */
    private double calculateStringSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算Levenshtein编辑距离
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
