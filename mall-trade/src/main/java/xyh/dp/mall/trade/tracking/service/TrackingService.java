package xyh.dp.mall.trade.tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.matching.engine.MatchScoreCalculator;
import xyh.dp.mall.trade.matching.feature.MatchFeature;
import xyh.dp.mall.trade.tracking.dto.TrackingEventDTO;
import xyh.dp.mall.trade.tracking.entity.UserTrackingEvent;
import xyh.dp.mall.trade.tracking.mapper.UserTrackingEventMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Set;

/**
 * 埋点服务
 * 用于收集和记录用户行为
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserTrackingEventMapper eventMapper;
    private final MatchScoreCalculator scoreCalculator;

    /**
     * 确认/购买相关的事件类型（正样本）
     */
    private static final Set<String> POSITIVE_EVENT_TYPES = Set.of(
            "MATCH_CONFIRM", "ORDER_CREATE", "ORDER_PAY"
    );

    /**
     * 记录埋点事件
     * 
     * @param dto 埋点事件数据
     */
    @Async("orderExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void trackEvent(TrackingEventDTO dto) {
        try {
            UserTrackingEvent event = UserTrackingEvent.builder()
                    .eventId(generateEventId())
                    .userId(UserContextHolder.getBusinessUserId())
                    .userType(getUserType())
                    .eventType(dto.getEventType())
                    .planId(dto.getPlanId())
                    .productId(dto.getProductId())
                    .supplierId(dto.getSupplierId())
                    .deviceType(dto.getDeviceType())
                    .channel(dto.getChannel())
                    .stayDuration(dto.getStayDuration())
                    .isPositive(POSITIVE_EVENT_TYPES.contains(dto.getEventType()) ? 1 : 0)
                    .eventTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .extData(dto.getExtData())
                    .build();

            eventMapper.insert(event);
            log.debug("埋点事件记录成功: eventId={}, eventType={}", event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("埋点事件记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录匹配相关埋点事件（带特征快照）
     * 
     * @param dto 埋点事件数据
     * @param plan 种植计划
     * @param product 商品信息
     */
    @Async("orderExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void trackMatchEvent(TrackingEventDTO dto, PlantingPlan plan, ProductDTO product) {
        try {
            // 计算匹配特征
            MatchFeature feature = scoreCalculator.calculateScore(plan, product);

            UserTrackingEvent event = UserTrackingEvent.builder()
                    .eventId(generateEventId())
                    .userId(UserContextHolder.getBusinessUserId())
                    .userType(getUserType())
                    .eventType(dto.getEventType())
                    .planId(dto.getPlanId())
                    .productId(dto.getProductId())
                    .supplierId(dto.getSupplierId())
                    // 匹配特征快照
                    .varietyScore(feature.getVarietyScore())
                    .regionScore(feature.getRegionScore())
                    .climateScore(feature.getClimateScore())
                    .seasonScore(feature.getSeasonScore())
                    .qualityScore(feature.getQualityScore())
                    .intentScore(feature.getIntentScore())
                    .totalScore(feature.getTotalScore())
                    .matchGrade(feature.getMatchGrade())
                    // 上下文信息
                    .deviceType(dto.getDeviceType())
                    .channel(dto.getChannel())
                    .stayDuration(dto.getStayDuration())
                    .isPositive(POSITIVE_EVENT_TYPES.contains(dto.getEventType()) ? 1 : 0)
                    .eventTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .extData(dto.getExtData())
                    .build();

            eventMapper.insert(event);
            log.info("匹配埋点事件记录成功: eventId={}, planId={}, productId={}, isPositive={}", 
                    event.getEventId(), dto.getPlanId(), dto.getProductId(), event.getIsPositive());
        } catch (Exception e) {
            log.error("匹配埋点事件记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录已计算好特征的匹配事件
     * 
     * @param dto 埋点事件数据
     * @param feature 匹配特征
     */
    @Async("orderExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void trackMatchEventWithFeature(TrackingEventDTO dto, MatchFeature feature) {
        try {
            UserTrackingEvent event = UserTrackingEvent.builder()
                    .eventId(generateEventId())
                    .userId(UserContextHolder.getBusinessUserId())
                    .userType(getUserType())
                    .eventType(dto.getEventType())
                    .planId(feature.getPlanId())
                    .productId(feature.getProductId())
                    .supplierId(dto.getSupplierId())
                    // 匹配特征快照
                    .varietyScore(feature.getVarietyScore())
                    .regionScore(feature.getRegionScore())
                    .climateScore(feature.getClimateScore())
                    .seasonScore(feature.getSeasonScore())
                    .qualityScore(feature.getQualityScore())
                    .intentScore(feature.getIntentScore())
                    .totalScore(feature.getTotalScore())
                    .matchGrade(feature.getMatchGrade())
                    // 上下文信息
                    .deviceType(dto.getDeviceType())
                    .channel(dto.getChannel())
                    .stayDuration(dto.getStayDuration())
                    .isPositive(POSITIVE_EVENT_TYPES.contains(dto.getEventType()) ? 1 : 0)
                    .eventTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .extData(dto.getExtData())
                    .build();

            eventMapper.insert(event);
            log.info("匹配埋点事件记录成功: eventId={}, planId={}, productId={}, totalScore={}, isPositive={}", 
                    event.getEventId(), feature.getPlanId(), feature.getProductId(), 
                    feature.getTotalScore(), event.getIsPositive());
        } catch (Exception e) {
            log.error("匹配埋点事件记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成事件ID
     * 
     * @return 事件ID
     */
    private String generateEventId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = new Random().nextInt(10000);
        return "EVT" + timestamp + String.format("%04d", random);
    }

    /**
     * 获取当前用户类型
     * 
     * @return 用户类型
     */
    private String getUserType() {
        try {
            String userType = UserContextHolder.getContext() != null ? 
                    UserContextHolder.getContext().getUserType() : null;
            return userType != null ? userType : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
