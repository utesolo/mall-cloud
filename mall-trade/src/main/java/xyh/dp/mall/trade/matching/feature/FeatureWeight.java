package xyh.dp.mall.trade.matching.feature;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 特征权重配置
 * 可通过配置文件动态调整各特征的权重
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "matching.feature.weight")
public class FeatureWeight {

    /**
     * 品种一致性权重 (默认25%)
     */
    private BigDecimal variety = new BigDecimal("0.25");

    /**
     * 区域适配权重 (默认20%)
     */
    private BigDecimal region = new BigDecimal("0.20");

    /**
     * 气候匹配权重 (默认15%)
     */
    private BigDecimal climate = new BigDecimal("0.15");

    /**
     * 季节匹配权重 (默认15%)
     */
    private BigDecimal season = new BigDecimal("0.15");

    /**
     * 种子质量权重 (默认15%)
     */
    private BigDecimal quality = new BigDecimal("0.15");

    /**
     * 供需意图权重 (默认10%)
     */
    private BigDecimal intent = new BigDecimal("0.10");

    /**
     * 验证权重总和是否为1
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        BigDecimal total = variety.add(region).add(climate).add(season).add(quality).add(intent);
        return total.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * 获取归一化后的权重
     * 如果权重总和不为1，则进行归一化
     * 
     * @return 归一化的权重
     */
    public FeatureWeight normalize() {
        BigDecimal total = variety.add(region).add(climate).add(season).add(quality).add(intent);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            total = BigDecimal.ONE;
        }
        FeatureWeight normalized = new FeatureWeight();
        normalized.setVariety(variety.divide(total, 4, java.math.RoundingMode.HALF_UP));
        normalized.setRegion(region.divide(total, 4, java.math.RoundingMode.HALF_UP));
        normalized.setClimate(climate.divide(total, 4, java.math.RoundingMode.HALF_UP));
        normalized.setSeason(season.divide(total, 4, java.math.RoundingMode.HALF_UP));
        normalized.setQuality(quality.divide(total, 4, java.math.RoundingMode.HALF_UP));
        normalized.setIntent(intent.divide(total, 4, java.math.RoundingMode.HALF_UP));
        return normalized;
    }
}
