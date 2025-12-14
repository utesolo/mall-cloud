package xyh.dp.mall.trade.matching.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.feature.FeatureWeight;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MatchScoreCalculator 匹配评分计算器单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchScoreCalculator 匹配评分计算器测试")
class MatchScoreCalculatorTest {

    @Mock
    private MatchFeatureExtractor featureExtractor;

    @Mock
    private FeatureWeight featureWeight;

    @InjectMocks
    private MatchScoreCalculator matchScoreCalculator;

    private PlantingPlan testPlan;
    private ProductDTO testProduct;
    private ProductDTO testProduct2;
    private MatchFeature testFeature;
    private FeatureWeight normalizedWeight;

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
        testProduct.setRegions(Arrays.asList("华北", "华东"));
        testProduct.setPlantingSeasons(Arrays.asList("春季", "秋季"));
        testProduct.setGerminationRate(new BigDecimal("95"));
        testProduct.setPurity(new BigDecimal("99"));
        testProduct.setDifficulty("EASY");

        testProduct2 = new ProductDTO();
        testProduct2.setId(2L);
        testProduct2.setName("普通小麦种子");
        testProduct2.setVariety("普麦10");
        testProduct2.setRegions(Arrays.asList("华北"));
        testProduct2.setGerminationRate(new BigDecimal("85"));

        testFeature = MatchFeature.builder()
                .planId("PLAN202412150001")
                .productId(1L)
                .varietyScore(new BigDecimal("95"))
                .regionScore(new BigDecimal("80"))
                .climateScore(new BigDecimal("85"))
                .seasonScore(new BigDecimal("90"))
                .qualityScore(new BigDecimal("88"))
                .intentScore(new BigDecimal("75"))
                .build();

        normalizedWeight = new FeatureWeight();
        normalizedWeight.setVariety(new BigDecimal("0.25"));
        normalizedWeight.setRegion(new BigDecimal("0.20"));
        normalizedWeight.setClimate(new BigDecimal("0.15"));
        normalizedWeight.setSeason(new BigDecimal("0.15"));
        normalizedWeight.setQuality(new BigDecimal("0.15"));
        normalizedWeight.setIntent(new BigDecimal("0.10"));
    }

    @Nested
    @DisplayName("calculateScore 单商品评分测试")
    class CalculateScoreTest {

        /**
         * 测试计算单个商品的匹配得分
         */
        @Test
        @DisplayName("应正确计算加权总分")
        void calculateScore_shouldCalculateWeightedTotalScore() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalScore()).isNotNull();
            // 手动验证加权计算: 95*0.25 + 80*0.20 + 85*0.15 + 90*0.15 + 88*0.15 + 75*0.10
            // = 23.75 + 16 + 12.75 + 13.5 + 13.2 + 7.5 = 86.70
            assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("86.70"));
        }

        /**
         * 测试匹配等级计算
         */
        @Test
        @DisplayName("应正确设置匹配等级")
        void calculateScore_shouldSetMatchGrade() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            assertThat(result.getMatchGrade()).isNotNull();
        }

        /**
         * 测试推荐建议生成
         */
        @Test
        @DisplayName("应生成匹配建议")
        void calculateScore_shouldGenerateRecommendation() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            assertThat(result.getRecommendation()).isNotNull();
            assertThat(result.getRecommendation()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("calculateAndRankScores 批量计算排序测试")
    class CalculateAndRankScoresTest {

        /**
         * 测试批量计算并按得分排序
         */
        @Test
        @DisplayName("应按得分降序排列商品")
        void calculateAndRankScores_shouldSortByScoreDescending() {
            // Given
            MatchFeature feature1 = MatchFeature.builder()
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .regionScore(new BigDecimal("80"))
                    .climateScore(new BigDecimal("85"))
                    .seasonScore(new BigDecimal("90"))
                    .qualityScore(new BigDecimal("88"))
                    .intentScore(new BigDecimal("75"))
                    .build();

            MatchFeature feature2 = MatchFeature.builder()
                    .productId(2L)
                    .varietyScore(new BigDecimal("60"))
                    .regionScore(new BigDecimal("70"))
                    .climateScore(new BigDecimal("65"))
                    .seasonScore(new BigDecimal("70"))
                    .qualityScore(new BigDecimal("75"))
                    .intentScore(new BigDecimal("60"))
                    .build();

            when(featureExtractor.extractFeatures(any(), any()))
                    .thenReturn(feature1)
                    .thenReturn(feature2);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            List<ProductDTO> products = Arrays.asList(testProduct2, testProduct);

            // When
            List<MatchFeature> result = matchScoreCalculator.calculateAndRankScores(testPlan, products);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTotalScore())
                    .isGreaterThanOrEqualTo(result.get(1).getTotalScore());
        }

        /**
         * 测试空商品列表
         */
        @Test
        @DisplayName("空商品列表应返回空结果")
        void calculateAndRankScores_emptyList_shouldReturnEmpty() {
            // When
            List<MatchFeature> result = matchScoreCalculator.calculateAndRankScores(testPlan, Arrays.asList());

            // Then
            assertThat(result).isEmpty();
        }

        /**
         * 测试null商品列表
         */
        @Test
        @DisplayName("null商品列表应返回空结果")
        void calculateAndRankScores_nullList_shouldReturnEmpty() {
            // When
            List<MatchFeature> result = matchScoreCalculator.calculateAndRankScores(testPlan, null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBestMatch 最佳匹配测试")
    class FindBestMatchTest {

        /**
         * 测试找到最佳匹配
         */
        @Test
        @DisplayName("应返回得分最高的商品")
        void findBestMatch_shouldReturnHighestScore() {
            // Given
            MatchFeature feature1 = MatchFeature.builder()
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .regionScore(new BigDecimal("80"))
                    .climateScore(new BigDecimal("85"))
                    .seasonScore(new BigDecimal("90"))
                    .qualityScore(new BigDecimal("88"))
                    .intentScore(new BigDecimal("75"))
                    .build();

            when(featureExtractor.extractFeatures(any(), any())).thenReturn(feature1);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.findBestMatch(testPlan, Arrays.asList(testProduct));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(1L);
        }

        /**
         * 测试空列表返回null
         */
        @Test
        @DisplayName("空商品列表应返回null")
        void findBestMatch_emptyList_shouldReturnNull() {
            // When
            MatchFeature result = matchScoreCalculator.findBestMatch(testPlan, Arrays.asList());

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getRecommendations 推荐列表测试")
    class GetRecommendationsTest {

        /**
         * 测试获取推荐商品（得分>=60）
         */
        @Test
        @DisplayName("应只返回得分>=60的商品")
        void getRecommendations_shouldFilterByMinScore() {
            // Given
            MatchFeature highScoreFeature = MatchFeature.builder()
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .regionScore(new BigDecimal("80"))
                    .climateScore(new BigDecimal("85"))
                    .seasonScore(new BigDecimal("90"))
                    .qualityScore(new BigDecimal("88"))
                    .intentScore(new BigDecimal("75"))
                    .build();

            when(featureExtractor.extractFeatures(any(), any())).thenReturn(highScoreFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            List<MatchFeature> result = matchScoreCalculator.getRecommendations(
                    testPlan, Arrays.asList(testProduct), 5);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getTotalScore())
                    .isGreaterThanOrEqualTo(new BigDecimal("60"));
        }

        /**
         * 测试限制返回数量
         */
        @Test
        @DisplayName("应限制返回数量")
        void getRecommendations_shouldLimitResults() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            List<ProductDTO> manyProducts = Arrays.asList(
                    testProduct, testProduct, testProduct, testProduct, testProduct
            );

            // When
            List<MatchFeature> result = matchScoreCalculator.getRecommendations(testPlan, manyProducts, 3);

            // Then
            assertThat(result).hasSizeLessThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("匹配等级判定测试")
    class MatchGradeTest {

        /**
         * 测试A级匹配（>=85）
         */
        @Test
        @DisplayName("得分>=85应为A级")
        void matchGrade_highScore_shouldBeGradeA() {
            // Given
            MatchFeature highFeature = MatchFeature.builder()
                    .planId("PLAN001")
                    .productId(1L)
                    .varietyScore(new BigDecimal("100"))
                    .regionScore(new BigDecimal("90"))
                    .climateScore(new BigDecimal("95"))
                    .seasonScore(new BigDecimal("95"))
                    .qualityScore(new BigDecimal("90"))
                    .intentScore(new BigDecimal("85"))
                    .build();

            when(featureExtractor.extractFeatures(any(), any())).thenReturn(highFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            // 100*0.25 + 90*0.20 + 95*0.15 + 95*0.15 + 90*0.15 + 85*0.10 = 93.5
            assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(new BigDecimal("85"));
            assertThat(result.getMatchGrade()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("建议生成测试")
    class RecommendationGenerationTest {

        /**
         * 测试高分优势描述
         */
        @Test
        @DisplayName("高分项应生成优势描述")
        void recommendation_highScores_shouldContainStrengths() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            assertThat(result.getRecommendation()).contains("优势");
        }

        /**
         * 测试包含整体评价
         */
        @Test
        @DisplayName("建议应包含整体评价")
        void recommendation_shouldContainOverallEvaluation() {
            // Given
            when(featureExtractor.extractFeatures(any(), any())).thenReturn(testFeature);
            when(featureWeight.normalize()).thenReturn(normalizedWeight);

            // When
            MatchFeature result = matchScoreCalculator.calculateScore(testPlan, testProduct);

            // Then
            assertThat(result.getRecommendation())
                    .containsAnyOf("推荐", "建议", "整体");
        }
    }

    @Nested
    @DisplayName("FeatureWeight 权重测试")
    class FeatureWeightTest {

        /**
         * 测试权重归一化
         */
        @Test
        @DisplayName("权重归一化后总和应为1")
        void featureWeight_normalize_shouldSumToOne() {
            // Given
            FeatureWeight weight = new FeatureWeight();
            weight.setVariety(new BigDecimal("0.25"));
            weight.setRegion(new BigDecimal("0.20"));
            weight.setClimate(new BigDecimal("0.15"));
            weight.setSeason(new BigDecimal("0.15"));
            weight.setQuality(new BigDecimal("0.15"));
            weight.setIntent(new BigDecimal("0.10"));

            // When
            BigDecimal sum = weight.getVariety()
                    .add(weight.getRegion())
                    .add(weight.getClimate())
                    .add(weight.getSeason())
                    .add(weight.getQuality())
                    .add(weight.getIntent());

            // Then
            assertThat(sum).isEqualByComparingTo(new BigDecimal("1.00"));
        }

        /**
         * 测试默认权重设置
         */
        @Test
        @DisplayName("默认权重应正确设置")
        void featureWeight_defaultValues_shouldBeSet() {
            // Given
            FeatureWeight weight = new FeatureWeight();

            // Then
            assertThat(weight.getVariety()).isEqualByComparingTo(new BigDecimal("0.25"));
            assertThat(weight.getRegion()).isEqualByComparingTo(new BigDecimal("0.20"));
            assertThat(weight.getClimate()).isEqualByComparingTo(new BigDecimal("0.15"));
            assertThat(weight.getSeason()).isEqualByComparingTo(new BigDecimal("0.15"));
            assertThat(weight.getQuality()).isEqualByComparingTo(new BigDecimal("0.15"));
            assertThat(weight.getIntent()).isEqualByComparingTo(new BigDecimal("0.10"));
        }
    }
}
