package xyh.dp.mall.trade.matching.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MatchFeatureExtractor 特征提取器单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchFeatureExtractor 特征提取器测试")
class MatchFeatureExtractorTest {

    @InjectMocks
    private MatchFeatureExtractor featureExtractor;

    private PlantingPlan testPlan;
    private ProductDTO testProduct;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testPlan = new PlantingPlan();
        testPlan.setPlanId("PLAN202412150001");
        testPlan.setFarmerId("FARMER001");
        testPlan.setVariety("济麦22");
        testPlan.setRegion("山东菏泽");
        testPlan.setPlantingDate(LocalDate.of(2025, 3, 15));
        testPlan.setTargetUsage("食用加工");

        testProduct = new ProductDTO();
        testProduct.setId(1L);
        testProduct.setName("优质小麦种子");
        testProduct.setVariety("济麦22");
        testProduct.setDescription("高产抗病小麦品种，适合食用加工");
        testProduct.setRegions(Arrays.asList("华北", "华东", "山东"));
        testProduct.setPlantingSeasons(Arrays.asList("春季", "秋季"));
        testProduct.setGerminationRate(new BigDecimal("95"));
        testProduct.setPurity(new BigDecimal("99"));
        testProduct.setDifficulty("EASY");
        testProduct.setMinTemperature(new BigDecimal("10"));
        testProduct.setMaxTemperature(new BigDecimal("25"));
        testProduct.setMinHumidity(new BigDecimal("40"));
        testProduct.setMaxHumidity(new BigDecimal("70"));
        testProduct.setLightRequirement("FULL_SUN");
    }

    @Nested
    @DisplayName("extractFeatures 特征提取测试")
    class ExtractFeaturesTest {

        /**
         * 测试提取完整特征向量
         */
        @Test
        @DisplayName("应提取完整的6维特征向量")
        void extractFeatures_shouldExtractAllSixFeatures() {
            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPlanId()).isEqualTo("PLAN202412150001");
            assertThat(result.getProductId()).isEqualTo(1L);
            assertThat(result.getVarietyScore()).isNotNull();
            assertThat(result.getRegionScore()).isNotNull();
            assertThat(result.getClimateScore()).isNotNull();
            assertThat(result.getSeasonScore()).isNotNull();
            assertThat(result.getQualityScore()).isNotNull();
            assertThat(result.getIntentScore()).isNotNull();
        }
    }

    @Nested
    @DisplayName("品种一致性特征测试")
    class VarietyScoreTest {

        /**
         * 测试完全匹配的品种
         */
        @Test
        @DisplayName("品种完全匹配应得满分")
        void varietyScore_exactMatch_shouldReturn100() {
            // Given
            testPlan.setVariety("济麦22");
            testProduct.setVariety("济麦22");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getVarietyScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试包含匹配的品种
         */
        @Test
        @DisplayName("品种包含匹配应得85分")
        void varietyScore_containsMatch_shouldReturn85() {
            // Given
            testPlan.setVariety("济麦22号");
            testProduct.setVariety("济麦22");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getVarietyScore()).isEqualByComparingTo(new BigDecimal("85"));
        }

        /**
         * 测试相似品种
         */
        @Test
        @DisplayName("相似品种应得相似度分数")
        void varietyScore_similarVariety_shouldReturnSimilarityScore() {
            // Given
            testPlan.setVariety("济麦20");
            testProduct.setVariety("济麦22");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getVarietyScore()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getVarietyScore()).isLessThan(new BigDecimal("100"));
        }

        /**
         * 测试空品种
         */
        @Test
        @DisplayName("空品种应得0分")
        void varietyScore_nullVariety_shouldReturn0() {
            // Given
            testPlan.setVariety(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getVarietyScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("区域适配特征测试")
    class RegionScoreTest {

        /**
         * 测试精确区域匹配
         */
        @Test
        @DisplayName("区域精确匹配应得满分")
        void regionScore_exactMatch_shouldReturn100() {
            // Given
            testPlan.setRegion("山东");
            testProduct.setRegions(Arrays.asList("山东", "河北"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getRegionScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试包含匹配
         */
        @Test
        @DisplayName("区域包含匹配应得满分")
        void regionScore_containsMatch_shouldReturn100() {
            // Given
            testPlan.setRegion("山东菏泽");
            testProduct.setRegions(Arrays.asList("山东", "河北"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getRegionScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试大区匹配
         */
        @Test
        @DisplayName("大区匹配应得80分")
        void regionScore_majorRegionMatch_shouldReturn80() {
            // Given
            testPlan.setRegion("山东菏泽");
            testProduct.setRegions(Arrays.asList("华北", "华东"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getRegionScore()).isEqualByComparingTo(new BigDecimal("80"));
        }

        /**
         * 测试无区域信息
         */
        @Test
        @DisplayName("无区域信息应得默认50分")
        void regionScore_noRegionInfo_shouldReturn50() {
            // Given
            testProduct.setRegions(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getRegionScore()).isEqualByComparingTo(new BigDecimal("50"));
        }

        /**
         * 测试空区域列表
         */
        @Test
        @DisplayName("空区域列表应得默认50分")
        void regionScore_emptyRegions_shouldReturn50() {
            // Given
            testProduct.setRegions(Collections.emptyList());

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getRegionScore()).isEqualByComparingTo(new BigDecimal("50"));
        }
    }

    @Nested
    @DisplayName("气候匹配特征测试")
    class ClimateScoreTest {

        /**
         * 测试气候完全适配
         */
        @Test
        @DisplayName("气候完全适配应得高分")
        void climateScore_perfectMatch_shouldReturnHighScore() {
            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getClimateScore()).isGreaterThanOrEqualTo(new BigDecimal("70"));
        }

        /**
         * 测试无温度信息
         */
        @Test
        @DisplayName("无温度信息时不扣分")
        void climateScore_noTemperatureInfo_shouldNotDeduct() {
            // Given
            testProduct.setMinTemperature(null);
            testProduct.setMaxTemperature(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getClimateScore()).isGreaterThanOrEqualTo(new BigDecimal("70"));
        }
    }

    @Nested
    @DisplayName("季节匹配特征测试")
    class SeasonScoreTest {

        /**
         * 测试春季匹配
         */
        @Test
        @DisplayName("春季种植匹配春季应得满分")
        void seasonScore_springMatch_shouldReturn100() {
            // Given
            testPlan.setPlantingDate(LocalDate.of(2025, 3, 15)); // 春季
            testProduct.setPlantingSeasons(Arrays.asList("春季", "秋季"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试夏季匹配
         */
        @Test
        @DisplayName("夏季种植匹配夏季应得满分")
        void seasonScore_summerMatch_shouldReturn100() {
            // Given
            testPlan.setPlantingDate(LocalDate.of(2025, 7, 15)); // 夏季
            testProduct.setPlantingSeasons(Arrays.asList("夏季"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试秋季匹配
         */
        @Test
        @DisplayName("秋季种植匹配秋季应得满分")
        void seasonScore_autumnMatch_shouldReturn100() {
            // Given
            testPlan.setPlantingDate(LocalDate.of(2025, 10, 15)); // 秋季
            testProduct.setPlantingSeasons(Arrays.asList("秋季"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试冬季匹配
         */
        @Test
        @DisplayName("冬季种植匹配冬季应得满分")
        void seasonScore_winterMatch_shouldReturn100() {
            // Given
            testPlan.setPlantingDate(LocalDate.of(2025, 1, 15)); // 冬季
            testProduct.setPlantingSeasons(Arrays.asList("冬季"));

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("100"));
        }

        /**
         * 测试相邻季节匹配
         */
        @Test
        @DisplayName("相邻季节应得70分")
        void seasonScore_adjacentSeason_shouldReturn70() {
            // Given
            testPlan.setPlantingDate(LocalDate.of(2025, 6, 15)); // 夏季
            testProduct.setPlantingSeasons(Arrays.asList("春季", "秋季")); // 相邻

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("70"));
        }

        /**
         * 测试无种植日期
         */
        @Test
        @DisplayName("无种植日期应得默认50分")
        void seasonScore_noPlantingDate_shouldReturn50() {
            // Given
            testPlan.setPlantingDate(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("50"));
        }

        /**
         * 测试无季节信息
         */
        @Test
        @DisplayName("无季节信息应得默认60分")
        void seasonScore_noSeasonInfo_shouldReturn60() {
            // Given
            testProduct.setPlantingSeasons(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getSeasonScore()).isEqualByComparingTo(new BigDecimal("60"));
        }
    }

    @Nested
    @DisplayName("种子质量特征测试")
    class QualityScoreTest {

        /**
         * 测试高质量种子
         */
        @Test
        @DisplayName("高发芽率高纯度应得高分")
        void qualityScore_highQuality_shouldReturnHighScore() {
            // Given
            testProduct.setGerminationRate(new BigDecimal("95"));
            testProduct.setPurity(new BigDecimal("99"));
            testProduct.setDifficulty("EASY");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            // 95*0.4 + 99*0.3 + 100*0.3 = 38 + 29.7 + 30 = 97.7
            assertThat(result.getQualityScore()).isGreaterThanOrEqualTo(new BigDecimal("85"));
        }

        /**
         * 测试中等难度
         */
        @Test
        @DisplayName("中等难度应降低质量分")
        void qualityScore_mediumDifficulty_shouldReduceScore() {
            // Given
            testProduct.setGerminationRate(new BigDecimal("95"));
            testProduct.setPurity(new BigDecimal("99"));
            testProduct.setDifficulty("MEDIUM");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            // 95*0.4 + 99*0.3 + 70*0.3 = 38 + 29.7 + 21 = 88.7
            assertThat(result.getQualityScore()).isLessThan(new BigDecimal("95"));
        }

        /**
         * 测试高难度
         */
        @Test
        @DisplayName("高难度应进一步降低质量分")
        void qualityScore_hardDifficulty_shouldReduceScoreMore() {
            // Given
            testProduct.setGerminationRate(new BigDecimal("95"));
            testProduct.setPurity(new BigDecimal("99"));
            testProduct.setDifficulty("HARD");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            // 95*0.4 + 99*0.3 + 50*0.3 = 38 + 29.7 + 15 = 82.7
            assertThat(result.getQualityScore()).isLessThan(new BigDecimal("90"));
        }

        /**
         * 测试无质量信息
         */
        @Test
        @DisplayName("无质量信息应得默认60分")
        void qualityScore_noQualityInfo_shouldReturn60() {
            // Given
            testProduct.setGerminationRate(null);
            testProduct.setPurity(null);
            testProduct.setDifficulty(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getQualityScore()).isEqualByComparingTo(new BigDecimal("60"));
        }
    }

    @Nested
    @DisplayName("供需意图特征测试")
    class IntentScoreTest {

        /**
         * 测试食用加工意图匹配
         */
        @Test
        @DisplayName("食用加工意图匹配应得高分")
        void intentScore_foodProcessingMatch_shouldReturnHighScore() {
            // Given
            testPlan.setTargetUsage("食用加工");
            testProduct.setDescription("高产抗病小麦品种，适合食用加工，营养丰富");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getIntentScore()).isGreaterThanOrEqualTo(new BigDecimal("80"));
        }

        /**
         * 测试工艺品制作意图匹配
         */
        @Test
        @DisplayName("工艺品制作意图匹配应得高分")
        void intentScore_craftMatch_shouldReturnHighScore() {
            // Given
            testPlan.setTargetUsage("工艺品制作");
            testProduct.setDescription("观赏价值高，适合工艺品制作和装饰");

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getIntentScore()).isGreaterThanOrEqualTo(new BigDecimal("80"));
        }

        /**
         * 测试无意图信息
         */
        @Test
        @DisplayName("无意图信息应得默认50分")
        void intentScore_noIntentInfo_shouldReturn50() {
            // Given
            testPlan.setTargetUsage(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getIntentScore()).isEqualByComparingTo(new BigDecimal("50"));
        }

        /**
         * 测试无商品描述
         */
        @Test
        @DisplayName("无商品描述应得默认60分")
        void intentScore_noDescription_shouldReturn60() {
            // Given
            testProduct.setDescription(null);

            // When
            MatchFeature result = featureExtractor.extractFeatures(testPlan, testProduct);

            // Then
            assertThat(result.getIntentScore()).isEqualByComparingTo(new BigDecimal("60"));
        }
    }

    @Nested
    @DisplayName("MatchFeature 实体测试")
    class MatchFeatureTest {

        /**
         * 测试Builder构建
         */
        @Test
        @DisplayName("MatchFeature Builder应正确构建")
        void matchFeature_builderShouldWork() {
            // Given
            MatchFeature feature = MatchFeature.builder()
                    .planId("PLAN001")
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .regionScore(new BigDecimal("80"))
                    .climateScore(new BigDecimal("85"))
                    .seasonScore(new BigDecimal("90"))
                    .qualityScore(new BigDecimal("88"))
                    .intentScore(new BigDecimal("75"))
                    .totalScore(new BigDecimal("85"))
                    .matchGrade("A")
                    .recommendation("推荐购买")
                    .build();

            // Then
            assertThat(feature.getPlanId()).isEqualTo("PLAN001");
            assertThat(feature.getProductId()).isEqualTo(1L);
            assertThat(feature.getVarietyScore()).isEqualByComparingTo(new BigDecimal("90"));
            assertThat(feature.getTotalScore()).isEqualByComparingTo(new BigDecimal("85"));
            assertThat(feature.getMatchGrade()).isEqualTo("A");
        }

        /**
         * 测试等级计算
         */
        @Test
        @DisplayName("calculateGrade应正确计算等级")
        void matchFeature_calculateGrade_shouldReturnCorrectGrade() {
            // Given - A级
            MatchFeature featureA = MatchFeature.builder()
                    .totalScore(new BigDecimal("90"))
                    .build();
            assertThat(featureA.calculateGrade()).isEqualTo("A");

            // Given - B级
            MatchFeature featureB = MatchFeature.builder()
                    .totalScore(new BigDecimal("75"))
                    .build();
            assertThat(featureB.calculateGrade()).isEqualTo("B");

            // Given - C级
            MatchFeature featureC = MatchFeature.builder()
                    .totalScore(new BigDecimal("59"))
                    .build();
            assertThat(featureC.calculateGrade()).isEqualTo("C");

            // Given - D级
            MatchFeature featureD = MatchFeature.builder()
                    .totalScore(new BigDecimal("50"))
                    .build();
            assertThat(featureD.calculateGrade()).isEqualTo("D");
        }

        /**
         * 测试CSV输出
         */
        @Test
        @DisplayName("toCsvLine应正确输出CSV格式")
        void matchFeature_toCsvLine_shouldReturnCsvFormat() {
            // Given
            MatchFeature feature = MatchFeature.builder()
                    .planId("PLAN202310010001")
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .regionScore(new BigDecimal("80"))
                    .climateScore(new BigDecimal("85"))
                    .seasonScore(new BigDecimal("90"))
                    .qualityScore(new BigDecimal("88"))
                    .intentScore(new BigDecimal("75"))
                    .build();

            // When
            String csvLine = feature.toCsvLine();

            // Then
            assertThat(csvLine).contains("90");
            assertThat(csvLine).contains("80");
            assertThat(csvLine).contains("85");
            assertThat(csvLine.split(",")).hasSize(10);
        }

        /**
         * 测试CSV头
         */
        @Test
        @DisplayName("csvHeader应返回正确的CSV头")
        void matchFeature_csvHeader_shouldReturnCorrectHeader() {
            // When
            String header = MatchFeature.csvHeader();

            // Then
            assertThat(header).contains("variety_score");
            assertThat(header).contains("region_score");
            assertThat(header).contains("climate_score");
            assertThat(header).contains("season_score");
            assertThat(header).contains("quality_score");
            assertThat(header).contains("intent_score");
        }
    }
}
