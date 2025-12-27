package xyh.dp.mall.trade.matching.engine;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * ML混合匹配服务
 * 支持规则引擎和机器学习模型的混合使用
 * 
 * 策略：
 * 1. 优先尝试调用ML模型API
 * 2. 如果ML API不可用 → 降级到规则引擎
 * 3. 支持灰度发布：可配置ML模型的流量比例
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLHybridMatchService {

    private final MatchScoreCalculator ruleEngineCalculator;
    private final RestTemplate restTemplate;
    
    /**
     * ML模型API地址（从配置文件读取）
     * 默认：http://localhost:5000
     */
    @Value("${ml.model.api-url:http://localhost:5000}")
    private String mlApiUrl;
    
    /**
     * ML模型是否启用（默认启用）
     */
    @Value("${ml.model.enabled:true}")
    private boolean mlEnabled;
    
    /**
     * ML模型流量比例（0-100）
     * 0：100%规则引擎，0%ML模型
     * 50：50%规则引擎，50%ML模型
     * 100：0%规则引擎，100%ML模型
     */
    @Value("${ml.model.traffic-ratio:0}")
    private int mlTrafficRatio;

    /**
     * 混合计算匹配得分
     * 根据配置决定使用ML或规则引擎
     * 
     * @param plan 种植计划
     * @param product 商品信息
     * @return 匹配特征
     */
    public MatchFeature calculateScore(PlantingPlan plan, ProductDTO product) {
        // 检查是否应该使用ML模型（基于流量比例）
        boolean useMl = shouldUseMl();
        
        if (useMl) {
            log.debug("使用ML模型计算匹配得分: planId={}, productId={}", 
                    plan.getPlanId(), product.getId());
            
            // 尝试调用ML模型
            MatchFeature mlFeature = calculateScoreWithML(plan, product);
            if (mlFeature != null) {
                log.info("ML模型预测成功: planId={}, productId={}, score={}", 
                        plan.getPlanId(), product.getId(), mlFeature.getTotalScore());
                return mlFeature;
            }
            
            // ML模型失败，降级到规则引擎
            log.warn("ML模型失败，降级到规则引擎: planId={}, productId={}", 
                    plan.getPlanId(), product.getId());
        }
        
        // 使用规则引擎计算
        log.debug("使用规则引擎计算匹配得分: planId={}, productId={}", 
                plan.getPlanId(), product.getId());
        return ruleEngineCalculator.calculateScore(plan, product);
    }

    /**
     * 使用ML模型计算匹配得分
     * 
     * @param plan 种植计划
     * @param product 商品信息
     * @return 匹配特征，失败返回null
     */
    private MatchFeature calculateScoreWithML(PlantingPlan plan, ProductDTO product) {
        if (!mlEnabled) {
            log.debug("ML模型已禁用");
            return null;
        }
        
        try {
            // 1. 先用规则引擎计算一次，获取完整特征（包含硬性约束判断）
            MatchFeature feature = ruleEngineCalculator.calculateScore(plan, product);

            // 如果存在硬性不匹配，直接返回规则引擎结果，不再调用ML
            if (ruleEngineCalculator.isHardMismatch(feature)) {
                log.debug("存在硬性不匹配，跳过ML模型: planId={}, productId={}",
                        plan.getPlanId(), product.getId());
                return feature;
            }
            
            // 2. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("variety_score", feature.getVarietyScore().doubleValue());
            requestBody.put("region_score", feature.getRegionScore().doubleValue());
            requestBody.put("climate_score", feature.getClimateScore().doubleValue());
            requestBody.put("season_score", feature.getSeasonScore().doubleValue());
            requestBody.put("quality_score", feature.getQualityScore().doubleValue());
            requestBody.put("intent_score", feature.getIntentScore().doubleValue());
            
            // 3. 调用ML API
            String predictUrl = mlApiUrl + "/api/predict";
            log.debug("调用ML API: url={}, feature={}", predictUrl, requestBody);
            
            String response = restTemplate.postForObject(predictUrl, requestBody, String.class);
            
            if (response == null) {
                log.error("ML API返回null");
                return null;
            }
            
            // 4. 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            if (jsonResponse.containsKey("error")) {
                log.error("ML API返回错误: {}", jsonResponse.getString("error"));
                return null;
            }
            
            // 5. 更新特征
            BigDecimal mlScore = new BigDecimal(jsonResponse.getDouble("score"));
            String mlGrade = jsonResponse.getString("grade");
            
            feature.setTotalScore(mlScore);
            feature.setMatchGrade(mlGrade);
            feature.setRecommendation(generateMLRecommendation(feature, jsonResponse));
            
            log.debug("ML模型预测结果: score={}, grade={}", mlScore, mlGrade);
            
            return feature;
            
        } catch (RestClientException e) {
            log.warn("调用ML API失败（连接错误）: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("调用ML API异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据ML模型响应生成建议
     * 
     * @param feature 匹配特征
     * @param jsonResponse ML模型的JSON响应
     * @return 建议文本
     */
    private String generateMLRecommendation(MatchFeature feature, JSONObject jsonResponse) {
        StringBuilder sb = new StringBuilder();
        
        // ML模型的置信度
        double confidence = jsonResponse.getDoubleValue("confidence");
        sb.append("ML模型预测置信度: ").append(String.format("%.2f%%", confidence * 100)).append("。");
        
        // 等级评价
        String grade = feature.getMatchGrade();
        switch (grade) {
            case "A":
                sb.append("ML模型预测：优秀匹配，强烈推荐。");
                break;
            case "B":
                sb.append("ML模型预测：良好匹配，建议选用。");
                break;
            case "C":
                sb.append("ML模型预测：一般匹配，可作为备选。");
                break;
            default:
                sb.append("ML模型预测：较弱匹配，建议谨慎选择。");
        }
        
        return sb.toString();
    }

    /**
     * 判断是否应该使用ML模型
     * 基于配置的流量比例和随机选择
     * 
     * @return true 使用ML模型，false 使用规则引擎
     */
    private boolean shouldUseMl() {
        if (!mlEnabled) {
            return false;
        }
        
        // 基于流量比例随机决定
        int random = (int) (Math.random() * 100);
        return random < mlTrafficRatio;
    }

    /**
     * 获取当前的模型使用配置
     * 
     * @return 配置信息
     */
    public MLModelConfig getModelConfig() {
        return MLModelConfig.builder()
                .mlEnabled(mlEnabled)
                .mlApiUrl(mlApiUrl)
                .mlTrafficRatio(mlTrafficRatio)
                .description(String.format(
                    "ML模型%s，流量比例: %d%%（%d%%规则引擎 + %d%%ML模型）",
                    mlEnabled ? "已启用" : "已禁用",
                    mlTrafficRatio,
                    100 - mlTrafficRatio,
                    mlTrafficRatio
                ))
                .build();
    }

    /**
     * 模型配置DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class MLModelConfig {
        private boolean mlEnabled;
        private String mlApiUrl;
        private int mlTrafficRatio;
        private String description;
    }
}
