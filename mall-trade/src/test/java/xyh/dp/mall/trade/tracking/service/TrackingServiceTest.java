package xyh.dp.mall.trade.tracking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.engine.MatchScoreCalculator;
import xyh.dp.mall.trade.matching.feature.MatchFeature;
import xyh.dp.mall.trade.tracking.dto.TrackingEventDTO;
import xyh.dp.mall.trade.tracking.entity.UserTrackingEvent;
import xyh.dp.mall.trade.tracking.mapper.UserTrackingEventMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TrackingService 埋点服务单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingService 埋点服务测试")
class TrackingServiceTest {

    @Mock
    private UserTrackingEventMapper eventMapper;

    @Mock
    private MatchScoreCalculator scoreCalculator;

    @InjectMocks
    private TrackingService trackingService;

    private TrackingEventDTO eventDTO;
    private PlantingPlan testPlan;
    private ProductDTO testProduct;
    private MatchFeature testMatchFeature;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        eventDTO = new TrackingEventDTO();
        eventDTO.setEventType("MATCH_VIEW");
        eventDTO.setPlanId("PLAN202412150001");
        eventDTO.setProductId(1L);
        eventDTO.setSupplierId("SUPPLY001");
        eventDTO.setDeviceType("WECHAT_MINI");
        eventDTO.setChannel("api");
        eventDTO.setStayDuration(5000);
        eventDTO.setExtData("{\"source\":\"recommendation\"}");

        testPlan = new PlantingPlan();
        testPlan.setId(1L);
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
                .build();
    }

    @Nested
    @DisplayName("trackEvent 基础埋点测试")
    class TrackEventTest {

        /**
         * 测试记录浏览事件
         */
        @Test
        @DisplayName("应成功记录浏览事件")
        void trackEvent_viewEvent_shouldInsertEvent() {
            // Given
            when(eventMapper.insert(any())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            verify(eventMapper, times(1)).insert(any());
        }

        /**
         * 测试正样本事件标记
         */
        @Test
        @DisplayName("确认事件应标记为正样本")
        void trackEvent_confirmEvent_shouldMarkAsPositive() {
            // Given
            eventDTO.setEventType("MATCH_CONFIRM");
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getIsPositive()).isEqualTo(1);
        }

        /**
         * 测试负样本事件标记
         */
        @Test
        @DisplayName("浏览事件应标记为负样本")
        void trackEvent_viewEvent_shouldMarkAsNegative() {
            // Given
            eventDTO.setEventType("MATCH_VIEW");
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getIsPositive()).isEqualTo(0);
        }

        /**
         * 测试点击事件标记为负样本
         */
        @Test
        @DisplayName("点击事件应标记为负样本")
        void trackEvent_clickEvent_shouldMarkAsNegative() {
            // Given
            eventDTO.setEventType("MATCH_CLICK");
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getIsPositive()).isEqualTo(0);
        }

        /**
         * 测试订单创建事件标记为正样本
         */
        @Test
        @DisplayName("订单创建事件应标记为正样本")
        void trackEvent_orderCreateEvent_shouldMarkAsPositive() {
            // Given
            eventDTO.setEventType("ORDER_CREATE");
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getIsPositive()).isEqualTo(1);
        }

        /**
         * 测试订单支付事件标记为正样本
         */
        @Test
        @DisplayName("订单支付事件应标记为正样本")
        void trackEvent_orderPayEvent_shouldMarkAsPositive() {
            // Given
            eventDTO.setEventType("ORDER_PAY");
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getIsPositive()).isEqualTo(1);
        }

        /**
         * 测试事件ID生成
         */
        @Test
        @DisplayName("应生成唯一的事件ID")
        void trackEvent_shouldGenerateUniqueEventId() {
            // Given
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackEvent(eventDTO);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getEventId()).isNotNull();
            assertThat(captured.getEventId()).startsWith("EVT");
        }
    }

    @Nested
    @DisplayName("trackMatchEvent 匹配埋点测试")
    class TrackMatchEventTest {

        /**
         * 测试记录匹配事件带特征快照
         */
        @Test
        @DisplayName("应记录匹配事件和特征快照")
        void trackMatchEvent_shouldInsertEventWithFeatures() {
            // Given
            when(scoreCalculator.calculateScore(any(), any())).thenReturn(testMatchFeature);
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackMatchEvent(eventDTO, testPlan, testProduct);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getVarietyScore()).isEqualByComparingTo(new BigDecimal("95"));
            assertThat(captured.getRegionScore()).isEqualByComparingTo(new BigDecimal("85"));
            assertThat(captured.getClimateScore()).isEqualByComparingTo(new BigDecimal("80"));
            assertThat(captured.getSeasonScore()).isEqualByComparingTo(new BigDecimal("90"));
            assertThat(captured.getQualityScore()).isEqualByComparingTo(new BigDecimal("88"));
            assertThat(captured.getIntentScore()).isEqualByComparingTo(new BigDecimal("75"));
            assertThat(captured.getTotalScore()).isEqualByComparingTo(new BigDecimal("86.50"));
            assertThat(captured.getMatchGrade()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("trackMatchEventWithFeature 带特征匹配埋点测试")
    class TrackMatchEventWithFeatureTest {

        /**
         * 测试使用预计算特征记录埋点
         */
        @Test
        @DisplayName("应使用预计算特征记录埋点")
        void trackMatchEventWithFeature_shouldUseProvidedFeatures() {
            // Given
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackMatchEventWithFeature(eventDTO, testMatchFeature);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getVarietyScore()).isEqualByComparingTo(new BigDecimal("95"));
            assertThat(captured.getTotalScore()).isEqualByComparingTo(new BigDecimal("86.50"));
            assertThat(captured.getPlanId()).isEqualTo("PLAN202412150001");
            assertThat(captured.getProductId()).isEqualTo(1L);
        }

        /**
         * 测试事件时间设置
         */
        @Test
        @DisplayName("应设置正确的事件时间")
        void trackMatchEventWithFeature_shouldSetEventTime() {
            // Given
            ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
            when(eventMapper.insert(captor.capture())).thenReturn(1);

            // When
            trackingService.trackMatchEventWithFeature(eventDTO, testMatchFeature);

            // Then
            UserTrackingEvent captured = captor.getValue();
            assertThat(captured.getEventTime()).isNotNull();
            assertThat(captured.getCreateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("正样本类型判断测试")
    class PositiveEventTypeTest {

        /**
         * 测试所有正样本类型
         */
        @Test
        @DisplayName("MATCH_CONFIRM/ORDER_CREATE/ORDER_PAY应为正样本")
        void positiveEventTypes_shouldAllBePositive() {
            // Given
            String[] positiveTypes = {"MATCH_CONFIRM", "ORDER_CREATE", "ORDER_PAY"};

            for (String eventType : positiveTypes) {
                eventDTO.setEventType(eventType);
                ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
                when(eventMapper.insert(captor.capture())).thenReturn(1);

                // When
                trackingService.trackEvent(eventDTO);

                // Then
                UserTrackingEvent captured = captor.getValue();
                assertThat(captured.getIsPositive())
                        .as("Event type %s should be positive", eventType)
                        .isEqualTo(1);
            }
        }

        /**
         * 测试所有负样本类型
         */
        @Test
        @DisplayName("MATCH_VIEW/MATCH_CLICK应为负样本")
        void negativeEventTypes_shouldAllBeNegative() {
            // Given
            String[] negativeTypes = {"MATCH_VIEW", "MATCH_CLICK", "PAGE_VIEW", "SEARCH"};

            for (String eventType : negativeTypes) {
                eventDTO.setEventType(eventType);
                ArgumentCaptor<UserTrackingEvent> captor = ArgumentCaptor.forClass(UserTrackingEvent.class);
                when(eventMapper.insert(captor.capture())).thenReturn(1);

                // When
                trackingService.trackEvent(eventDTO);

                // Then
                UserTrackingEvent captured = captor.getValue();
                assertThat(captured.getIsPositive())
                        .as("Event type %s should be negative", eventType)
                        .isEqualTo(0);
            }
        }
    }

    @Nested
    @DisplayName("TrackingEventDTO 验证测试")
    class TrackingEventDTOTest {

        /**
         * 测试DTO字段设置
         */
        @Test
        @DisplayName("TrackingEventDTO应正确设置所有字段")
        void trackingEventDTO_shouldSetAllFields() {
            // Given
            TrackingEventDTO dto = new TrackingEventDTO();
            dto.setEventType("MATCH_VIEW");
            dto.setPlanId("PLAN001");
            dto.setProductId(1L);
            dto.setSupplierId("SUPPLY001");
            dto.setDeviceType("WECHAT_MINI");
            dto.setChannel("api");
            dto.setStayDuration(3000);
            dto.setExtData("{\"key\":\"value\"}");

            // Then
            assertThat(dto.getEventType()).isEqualTo("MATCH_VIEW");
            assertThat(dto.getPlanId()).isEqualTo("PLAN001");
            assertThat(dto.getProductId()).isEqualTo(1L);
            assertThat(dto.getSupplierId()).isEqualTo("SUPPLY001");
            assertThat(dto.getDeviceType()).isEqualTo("WECHAT_MINI");
            assertThat(dto.getChannel()).isEqualTo("api");
            assertThat(dto.getStayDuration()).isEqualTo(3000);
            assertThat(dto.getExtData()).isEqualTo("{\"key\":\"value\"}");
        }
    }

    @Nested
    @DisplayName("UserTrackingEvent 实体测试")
    class UserTrackingEventTest {

        /**
         * 测试实体Builder
         */
        @Test
        @DisplayName("UserTrackingEvent Builder应正确构建实体")
        void userTrackingEvent_builderShouldWork() {
            // Given
            UserTrackingEvent event = UserTrackingEvent.builder()
                    .eventId("EVT202412150001")
                    .userId(1L)
                    .userType("FARMER")
                    .eventType("MATCH_VIEW")
                    .planId("PLAN001")
                    .productId(1L)
                    .varietyScore(new BigDecimal("90"))
                    .totalScore(new BigDecimal("85"))
                    .isPositive(0)
                    .eventTime(LocalDateTime.now())
                    .build();

            // Then
            assertThat(event.getEventId()).isEqualTo("EVT202412150001");
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getEventType()).isEqualTo("MATCH_VIEW");
            assertThat(event.getVarietyScore()).isEqualByComparingTo(new BigDecimal("90"));
            assertThat(event.getIsPositive()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {

        /**
         * 测试数据库异常不影响主流程
         */
        @Test
        @DisplayName("数据库异常应被捕获不影响主流程")
        void trackEvent_dbException_shouldNotThrow() {
            // Given
            when(eventMapper.insert(any())).thenThrow(new RuntimeException("DB Error"));

            // When/Then - should not throw
            trackingService.trackEvent(eventDTO);
        }
    }
}
