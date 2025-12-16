package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreatePlantingPlanDTO;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.PlantingPlanMapper;
import xyh.dp.mall.trade.matching.engine.MatchScoreCalculator;
import xyh.dp.mall.trade.matching.feature.MatchFeature;
import xyh.dp.mall.trade.tracking.service.TrackingService;
import xyh.dp.mall.trade.vo.PlantingPlanVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PlantingPlanService 种植计划服务单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlantingPlanService 种植计划服务测试")
class PlantingPlanServiceTest {

    @Mock
    private PlantingPlanMapper plantingPlanMapper;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private MatchScoreCalculator matchScoreCalculator;

    @Mock
    private TrackingService trackingService;

    @InjectMocks
    private PlantingPlanService plantingPlanService;

    private CreatePlantingPlanDTO createDTO;
    private PlantingPlan testPlan;
    private ProductDTO testProduct;
    private MatchFeature testMatchFeature;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        createDTO = new CreatePlantingPlanDTO();
        createDTO.setFarmerId("FARMER001");
        createDTO.setPlantingArea(BigDecimal.valueOf(10.5));
        createDTO.setVariety("济麦22");
        createDTO.setExpectedYield(5000);
        createDTO.setPlantingDate(LocalDate.of(2025, 3, 15));
        createDTO.setTargetUsage("食用加工");
        createDTO.setRegion("山东菏泽");

        testPlan = new PlantingPlan();
        testPlan.setId(1L);
        testPlan.setPlanId("PLAN202412150001");
        testPlan.setFarmerId("FARMER001");
        testPlan.setPlantingArea(BigDecimal.valueOf(10.5));
        testPlan.setVariety("济麦22");
        testPlan.setExpectedYield(5000);
        testPlan.setPlantingDate(LocalDate.of(2025, 3, 15));
        testPlan.setTargetUsage("食用加工");
        testPlan.setRegion("山东菏泽");
        testPlan.setPlanSummary("山东菏泽·济麦22·10.5亩·预计产量5000个·用于食用加工");
        testPlan.setMatchStatus("PENDING");
        testPlan.setCreateTime(LocalDateTime.now());
        testPlan.setUpdateTime(LocalDateTime.now());

        testProduct = new ProductDTO();
        testProduct.setId(1L);
        testProduct.setName("优质小麦种子");
        testProduct.setVariety("济麦22");
        testProduct.setRegions(Arrays.asList("华北", "华东"));
        testProduct.setPrice(new BigDecimal("25.00"));
        testProduct.setSupplierId(1L);

        testMatchFeature = MatchFeature.builder()
                .planId("PLAN202412150001")
                .productId(1L)
                .varietyScore(new BigDecimal("95"))
                .regionScore(new BigDecimal("85"))
                .climateScore(new BigDecimal("80"))
                .seasonScore(new BigDecimal("90"))
                .qualityScore(new BigDecimal("88"))
                .intentScore(new BigDecimal("75"))
                .totalScore(new BigDecimal("86.50"))
                .matchGrade("A")
                .recommendation("品种高度匹配，整体匹配度优秀")
                .build();
    }

    @Nested
    @DisplayName("createPlan 创建种植计划测试")
    class CreatePlanTest {

        /**
         * 测试正常创建种植计划
         */
        @Test
        @DisplayName("应成功创建种植计划")
        void createPlan_shouldSucceed() {
            // Given
            when(plantingPlanMapper.insert((PlantingPlan) any())).thenReturn(1);

            // When
            PlantingPlanVO result = plantingPlanService.createPlan(createDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFarmerId()).isEqualTo("FARMER001");
            assertThat(result.getVariety()).isEqualTo("济麦22");
            assertThat(result.getPlantingArea()).isEqualTo(10.5);
            assertThat(result.getMatchStatus()).isEqualTo("PENDING");
            assertThat(result.getPlanSummary()).contains("山东菏泽");
            assertThat(result.getPlanSummary()).contains("济麦22");
            verify(plantingPlanMapper, times(1)).insert((PlantingPlan) any());
        }

        /**
         * 测试计划摘要生成
         */
        @Test
        @DisplayName("应正确生成计划摘要")
        void createPlan_shouldGenerateCorrectSummary() {
            // Given
            when(plantingPlanMapper.insert((PlantingPlan) any())).thenReturn(1);

            // When
            PlantingPlanVO result = plantingPlanService.createPlan(createDTO);

            // Then
            assertThat(result.getPlanSummary()).isEqualTo("山东菏泽·济麦22·10.5亩·预计产量5000个·用于食用加工");
        }
    }

    @Nested
    @DisplayName("getByPlanIdVO 查询种植计划测试")
    class GetByPlanIdVOTest {

        /**
         * 测试计划存在时正常返回
         */
        @Test
        @DisplayName("计划存在时应返回计划详情")
        void getByPlanIdVO_existingPlan_shouldReturnPlanVO() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When
            PlantingPlanVO result = plantingPlanService.getByPlanIdVO("PLAN202412150001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPlanId()).isEqualTo("PLAN202412150001");
            assertThat(result.getFarmerId()).isEqualTo("FARMER001");
            assertThat(result.getVariety()).isEqualTo("济麦22");
        }

        /**
         * 测试计划不存在时抛出异常
         */
        @Test
        @DisplayName("计划不存在时应抛出BusinessException")
        void getByPlanIdVO_nonExistingPlan_shouldThrowException() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.getByPlanIdVO("PLAN999999999999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("种植计划不存在");
        }
    }

    @Nested
    @DisplayName("executeMatch 执行匹配测试")
    class ExecuteMatchTest {

        /**
         * 测试已匹配的计划不能重复匹配
         */
        @Test
        @DisplayName("已匹配的计划不能重复匹配")
        void executeMatch_alreadyMatched_shouldThrowException() {
            // Given
            testPlan.setMatchStatus("MATCHED");
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.executeMatch("PLAN202412150001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("该计划已匹配或已取消");
        }

        /**
         * 测试无候选商品时回退到模拟匹配
         */
        @Test
        @DisplayName("无候选商品时应回退到模拟匹配")
        void executeMatch_noCandidates_shouldFallbackToSimulation() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(productFeignClient.searchProducts(anyString(), anyString(), anyInt()))
                    .thenReturn(Result.success(Arrays.asList()));
            when(matchScoreCalculator.findBestMatch(any(), any())).thenReturn(null);
            when(plantingPlanMapper.updateById((PlantingPlan) any())).thenReturn(1);

            // When
            PlantingPlanVO result = plantingPlanService.executeMatch("PLAN202412150001");

            // Then
            assertThat(result.getMatchStatus()).isEqualTo("MATCHED");
            assertThat(result.getMatchScore()).isBetween(60, 100);
            assertThat(result.getSupplierId()).startsWith("SUPPLY");
        }
    }

    @Nested
    @DisplayName("confirmMatch 确认匹配测试")
    class ConfirmMatchTest {

        /**
         * 测试正常确认匹配
         */
        @Test
        @DisplayName("匹配的计划应能成功确认")
        void confirmMatch_matchedPlan_shouldSucceed() {
            // Given
            testPlan.setMatchStatus("MATCHED");
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(plantingPlanMapper.updateById((PlantingPlan) any())).thenReturn(1);

            // When
            plantingPlanService.confirmMatch("PLAN202412150001", "FARMER001");

            // Then
            verify(plantingPlanMapper, times(1)).updateById((PlantingPlan) any());
        }

        /**
         * 测试非本人操作
         */
        @Test
        @DisplayName("非本人不能确认匹配")
        void confirmMatch_differentFarmer_shouldThrowException() {
            // Given
            testPlan.setMatchStatus("MATCHED");
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.confirmMatch("PLAN202412150001", "FARMER999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无权操作此计划");
        }

        /**
         * 测试未匹配的计划不能确认
         */
        @Test
        @DisplayName("未匹配的计划不能确认")
        void confirmMatch_pendingPlan_shouldThrowException() {
            // Given
            testPlan.setMatchStatus("PENDING");
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.confirmMatch("PLAN202412150001", "FARMER001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("只能确认已匹配的计划");
        }
    }

    @Nested
    @DisplayName("cancelPlan 取消计划测试")
    class CancelPlanTest {

        /**
         * 测试正常取消待匹配计划
         */
        @Test
        @DisplayName("待匹配计划应能成功取消")
        void cancelPlan_pendingPlan_shouldSucceed() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(plantingPlanMapper.updateById((PlantingPlan) any())).thenReturn(1);

            // When
            plantingPlanService.cancelPlan("PLAN202412150001", "FARMER001");

            // Then
            verify(plantingPlanMapper, times(1)).updateById((PlantingPlan) any());
        }

        /**
         * 测试已确认的计划不能取消
         */
        @Test
        @DisplayName("已确认的计划不能取消")
        void cancelPlan_confirmedPlan_shouldThrowException() {
            // Given
            testPlan.setMatchStatus("CONFIRMED");
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.cancelPlan("PLAN202412150001", "FARMER001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("已确认的计划无法取消");
        }

        /**
         * 测试非本人操作
         */
        @Test
        @DisplayName("非本人不能取消计划")
        void cancelPlan_differentFarmer_shouldThrowException() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.cancelPlan("PLAN202412150001", "FARMER999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无权操作此计划");
        }
    }

    @Nested
    @DisplayName("pageQueryByFarmer 农户计划分页查询测试")
    class PageQueryByFarmerTest {

        /**
         * 测试分页查询正常返回
         */
        @Test
        @DisplayName("应正确返回农户的计划列表")
        void pageQueryByFarmer_shouldReturnPagedPlans() {
            // Given
            Page<PlantingPlan> planPage = new Page<>(1, 10);
            planPage.setRecords(Arrays.asList(testPlan));
            planPage.setTotal(1);
            
            when(plantingPlanMapper.selectPage(any(), any())).thenReturn(planPage);

            // When
            Page<PlantingPlanVO> result = plantingPlanService.pageQueryByFarmer("FARMER001", 1, 10, null);

            // Then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getFarmerId()).isEqualTo("FARMER001");
        }

        /**
         * 测试按状态筛选
         */
        @Test
        @DisplayName("应支持按状态筛选计划")
        void pageQueryByFarmer_withStatus_shouldFilterByStatus() {
            // Given
            Page<PlantingPlan> planPage = new Page<>(1, 10);
            planPage.setRecords(Arrays.asList(testPlan));
            planPage.setTotal(1);
            
            when(plantingPlanMapper.selectPage(any(), any())).thenReturn(planPage);

            // When
            Page<PlantingPlanVO> result = plantingPlanService.pageQueryByFarmer("FARMER001", 1, 10, "PENDING");

            // Then
            assertThat(result.getRecords()).hasSize(1);
            verify(plantingPlanMapper, times(1)).selectPage(any(), any());
        }
    }

    @Nested
    @DisplayName("pageQueryForSupplier 供销商计划分页查询测试")
    class PageQueryForSupplierTest {

        /**
         * 测试供销商查询匹配的计划
         */
        @Test
        @DisplayName("应返回分配给供销商的计划")
        void pageQueryForSupplier_shouldReturnMatchedPlans() {
            // Given
            testPlan.setMatchStatus("MATCHED");
            testPlan.setSupplierId("SUPPLY001");
            
            Page<PlantingPlan> planPage = new Page<>(1, 10);
            planPage.setRecords(Arrays.asList(testPlan));
            planPage.setTotal(1);
            
            when(plantingPlanMapper.selectPage(any(), any())).thenReturn(planPage);

            // When
            Page<PlantingPlanVO> result = plantingPlanService.pageQueryForSupplier("SUPPLY001", 1, 10);

            // Then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getSupplierId()).isEqualTo("SUPPLY001");
        }
    }

    @Nested
    @DisplayName("getRecommendedProducts 获取推荐商品测试")
    class GetRecommendedProductsTest {

        /**
         * 测试获取推荐商品列表
         */
        @Test
        @DisplayName("应返回排序后的推荐商品列表")
        void getRecommendedProducts_shouldReturnRankedList() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(productFeignClient.searchProducts(anyString(), anyString(), anyInt()))
                    .thenReturn(Result.success(Arrays.asList(testProduct)));
            when(matchScoreCalculator.getRecommendations(any(), any(), anyInt()))
                    .thenReturn(Arrays.asList(testMatchFeature));

            // When
            List<MatchFeature> result = plantingPlanService.getRecommendedProducts("PLAN202412150001", 5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTotalScore()).isEqualByComparingTo(new BigDecimal("86.50"));
        }

        /**
         * 测试无推荐商品时返回空列表
         */
        @Test
        @DisplayName("无推荐商品时应返回空列表")
        void getRecommendedProducts_noCandidates_shouldReturnEmptyList() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(productFeignClient.searchProducts(anyString(), anyString(), anyInt()))
                    .thenReturn(Result.success(Arrays.asList()));

            // When
            List<MatchFeature> result = plantingPlanService.getRecommendedProducts("PLAN202412150001", 5);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("clickProduct 点击商品测试")
    class ClickProductTest {

        /**
         * 测试点击商品记录埋点
         */
        @Test
        @DisplayName("点击商品应计算匹配分数并记录埋点")
        void clickProduct_shouldCalculateScoreAndTrack() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(productFeignClient.getProductById(1L)).thenReturn(Result.success(testProduct));
            when(matchScoreCalculator.calculateScore(any(), any())).thenReturn(testMatchFeature);

            // When
            MatchFeature result = plantingPlanService.clickProduct("PLAN202412150001", 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("86.50"));
            verify(trackingService, times(1)).trackMatchEventWithFeature(any(), any());
        }

        /**
         * 测试点击不存在的商品
         */
        @Test
        @DisplayName("商品不存在时应抛出异常")
        void clickProduct_nonExistingProduct_shouldThrowException() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);
            when(productFeignClient.getProductById(999L)).thenReturn(Result.success(null));

            // When/Then
            assertThatThrownBy(() -> plantingPlanService.clickProduct("PLAN202412150001", 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("商品不存在");
        }
    }

    @Nested
    @DisplayName("PlantingPlanVO 转换测试")
    class PlantingPlanVOConversionTest {

        /**
         * 测试VO包含所有字段
         */
        @Test
        @DisplayName("PlantingPlanVO应包含所有计划信息")
        void plantingPlanVO_shouldContainAllFields() {
            // Given
            when(plantingPlanMapper.selectOne(any())).thenReturn(testPlan);

            // When
            PlantingPlanVO result = plantingPlanService.getByPlanIdVO("PLAN202412150001");

            // Then
            assertThat(result.getPlanId()).isEqualTo("PLAN202412150001");
            assertThat(result.getFarmerId()).isEqualTo("FARMER001");
            assertThat(result.getPlantingArea()).isEqualTo(10.5);
            assertThat(result.getVariety()).isEqualTo("济麦22");
            assertThat(result.getExpectedYield()).isEqualTo(5000);
            assertThat(result.getPlantingDate()).isEqualTo(LocalDate.of(2025, 3, 15));
            assertThat(result.getTargetUsage()).isEqualTo("食用加工");
            assertThat(result.getRegion()).isEqualTo("山东菏泽");
            assertThat(result.getMatchStatus()).isEqualTo("PENDING");
        }
    }
}
