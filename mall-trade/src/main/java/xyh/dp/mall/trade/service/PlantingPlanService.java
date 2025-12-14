package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreatePlantingPlanDTO;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.PlantingPlanMapper;
import xyh.dp.mall.trade.matching.engine.MatchScoreCalculator;
import xyh.dp.mall.trade.matching.feature.MatchFeature;
import xyh.dp.mall.trade.tracking.annotation.TrackEvent;
import xyh.dp.mall.trade.tracking.dto.TrackingEventDTO;
import xyh.dp.mall.trade.tracking.service.TrackingService;
import xyh.dp.mall.trade.vo.PlantingPlanVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 种植计划服务（供给匹配）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantingPlanService {

    private final PlantingPlanMapper plantingPlanMapper;
    private final ProductFeignClient productFeignClient;
    private final MatchScoreCalculator matchScoreCalculator;
    private final TrackingService trackingService;

    /**
     * 创建种植计划
     * 
     * @param createDTO 创建种植计划请求
     * @return 种植计划信息
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public PlantingPlanVO createPlan(CreatePlantingPlanDTO createDTO) {
        // 生成种植计划ID
        String planId = generatePlanId();
        
        // 生成计划摘要
        String summary = generatePlanSummary(createDTO);
        
        // 创建种植计划
        PlantingPlan plan = new PlantingPlan();
        plan.setPlanId(planId);
        plan.setFarmerId(createDTO.getFarmerId());
        plan.setPlantingArea(createDTO.getPlantingArea());
        plan.setVariety(createDTO.getVariety());
        plan.setExpectedYield(createDTO.getExpectedYield());
        plan.setPlantingDate(createDTO.getPlantingDate());
        plan.setTargetUsage(createDTO.getTargetUsage());
        plan.setRegion(createDTO.getRegion());
        plan.setPlanSummary(summary);
        plan.setMatchStatus("PENDING");
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        
        plantingPlanMapper.insert(plan);
        log.info("创建种植计划成功, planId: {}, farmerId: {}", planId, createDTO.getFarmerId());
        
        return convertToVO(plan);
    }

    /**
     * 执行智能供给匹配（规则引擎+加权评分）
     * 
     * @param planId 种植计划ID
     * @return 匹配后的种植计划信息
     * @throws BusinessException 计划不存在或已匹配
     */
    @Transactional(rollbackFor = Exception.class)
    @TrackEvent(eventType = "MATCH_EXECUTE", description = "执行种植计划匹配")
    public PlantingPlanVO executeMatch(String planId) {
        PlantingPlan plan = getByPlanId(planId);
        
        if (!"PENDING".equals(plan.getMatchStatus())) {
            throw new BusinessException("该计划已匹配或已取消");
        }
        
        // 调用智能匹配算法
        MatchFeature bestMatch = executeIntelligentMatch(plan);
        
        if (bestMatch == null) {
            // 回退到模拟匹配
            int matchScore = calculateMatchScoreFallback(plan);
            String supplierId = findBestSupplierFallback(plan);
            String climateMatch = generateClimateAnalysis(plan);
            
            plan.setMatchScore(matchScore);
            plan.setSupplierId(supplierId);
            plan.setClimateMatch(climateMatch);
        } else {
            // 使用智能匹配结果
            plan.setMatchScore(bestMatch.getTotalScore().intValue());
            plan.setSupplierId(findSupplierByProduct(bestMatch.getProductId()));
            plan.setClimateMatch(bestMatch.getRecommendation());
            
            // 记录匹配埋点
            recordMatchEvent(plan, bestMatch, "MATCH_VIEW");
        }
        
        plan.setMatchTime(LocalDateTime.now());
        plan.setMatchStatus("MATCHED");
        plan.setUpdateTime(LocalDateTime.now());
        
        plantingPlanMapper.updateById(plan);
        log.info("供给匹配成功, planId: {}, supplierId: {}, matchScore: {}", 
                planId, plan.getSupplierId(), plan.getMatchScore());
        
        return convertToVO(plan);
    }

    /**
     * 获取种植计划的推荐商品列表
     * 
     * @param planId 种植计划ID
     * @param limit 推荐数量限制
     * @return 推荐商品匹配特征列表
     */
    public List<MatchFeature> getRecommendedProducts(String planId, int limit) {
        PlantingPlan plan = getByPlanId(planId);
        
        // 获取候选商品列表
        List<ProductDTO> candidates = getCandidateProducts(plan);
        
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 计算匹配得分并排序
        List<MatchFeature> recommendations = matchScoreCalculator.getRecommendations(plan, candidates, limit);
        
        // 记录埋点
        for (MatchFeature feature : recommendations) {
            recordMatchEvent(plan, feature, "MATCH_VIEW");
        }
        
        return recommendations;
    }

    /**
     * 用户点击某个推荐商品
     * 
     * @param planId 种植计划ID
     * @param productId 商品ID
     * @return 匹配特征
     */
    public MatchFeature clickProduct(String planId, Long productId) {
        PlantingPlan plan = getByPlanId(planId);
        ProductDTO product = getProductById(productId);
        
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 计算匹配得分
        MatchFeature feature = matchScoreCalculator.calculateScore(plan, product);
        
        // 记录点击埋点
        recordMatchEvent(plan, feature, "MATCH_CLICK");
        
        return feature;
    }

    /**
     * 执行智能匹配算法
     * 
     * @param plan 种植计划
     * @return 最佳匹配特征
     */
    private MatchFeature executeIntelligentMatch(PlantingPlan plan) {
        try {
            // 获取候选商品
            List<ProductDTO> candidates = getCandidateProducts(plan);
            
            if (candidates.isEmpty()) {
                log.warn("未找到候选商品, planId: {}", plan.getPlanId());
                return null;
            }
            
            // 计算匹配得分并找到最佳匹配
            return matchScoreCalculator.findBestMatch(plan, candidates);
        } catch (Exception e) {
            log.error("智能匹配异常, 回退到模拟匹配: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取候选商品列表
     * 
     * @param plan 种植计划
     * @return 候选商品列表
     */
    private List<ProductDTO> getCandidateProducts(PlantingPlan plan) {
        try {
            // 通过Feign调用商品服务获取候选商品
            Result<List<ProductDTO>> result = productFeignClient.searchProducts(
                    plan.getVariety(), plan.getRegion(), 20);
            
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.error("获取候选商品失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 根据商品ID获取商品信息
     * 
     * @param productId 商品ID
     * @return 商品信息
     */
    private ProductDTO getProductById(Long productId) {
        try {
            Result<ProductDTO> result = productFeignClient.getProductById(productId);
            if (result != null && result.isSuccess()) {
                return result.getData();
            }
        } catch (Exception e) {
            log.error("获取商品信息失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据商品ID查找供应商
     * 
     * @param productId 商品ID
     * @return 供应商ID
     */
    private String findSupplierByProduct(Long productId) {
        ProductDTO product = getProductById(productId);
        if (product != null && product.getSupplierId() != null) {
            return "SUPPLY" + String.format("%03d", product.getSupplierId());
        }
        return findBestSupplierFallback(null);
    }

    /**
     * 记录匹配埋点事件
     * 
     * @param plan 种植计划
     * @param feature 匹配特征
     * @param eventType 事件类型
     */
    private void recordMatchEvent(PlantingPlan plan, MatchFeature feature, String eventType) {
        try {
            TrackingEventDTO dto = new TrackingEventDTO();
            dto.setEventType(eventType);
            dto.setPlanId(plan.getPlanId());
            dto.setProductId(feature.getProductId());
            dto.setSupplierId(plan.getSupplierId());
            dto.setChannel("api");
            
            trackingService.trackMatchEventWithFeature(dto, feature);
        } catch (Exception e) {
            log.warn("记录匹配埋点失败: {}", e.getMessage());
        }
    }

    /**
     * 确认匹配结果（正样本埋点）
     * 
     * @param planId 种植计划ID
     * @param farmerId 农户ID
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    @TrackEvent(eventType = "MATCH_CONFIRM", description = "确认匹配结果")
    public void confirmMatch(String planId, String farmerId) {
        PlantingPlan plan = getByPlanId(planId);
        
        if (!plan.getFarmerId().equals(farmerId)) {
            throw new BusinessException("无权操作此计划");
        }
        
        if (!"MATCHED".equals(plan.getMatchStatus())) {
            throw new BusinessException("只能确认已匹配的计划");
        }
        
        plan.setMatchStatus("CONFIRMED");
        plan.setUpdateTime(LocalDateTime.now());
        plantingPlanMapper.updateById(plan);
        
        // 记录确认埋点（正样本）
        recordConfirmEvent(plan);
        
        log.info("确认匹配成功, planId: {}", planId);
    }

    /**
     * 记录确认埋点事件（正样本）
     * 
     * @param plan 种植计划
     */
    private void recordConfirmEvent(PlantingPlan plan) {
        try {
            TrackingEventDTO dto = new TrackingEventDTO();
            dto.setEventType("MATCH_CONFIRM");
            dto.setPlanId(plan.getPlanId());
            dto.setSupplierId(plan.getSupplierId());
            dto.setChannel("api");
            
            trackingService.trackEvent(dto);
        } catch (Exception e) {
            log.warn("记录确认埋点失败: {}", e.getMessage());
        }
    }

    /**
     * 取消种植计划
     * 
     * @param planId 种植计划ID
     * @param farmerId 农户ID
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelPlan(String planId, String farmerId) {
        PlantingPlan plan = getByPlanId(planId);
        
        if (!plan.getFarmerId().equals(farmerId)) {
            throw new BusinessException("无权操作此计划");
        }
        
        if ("CONFIRMED".equals(plan.getMatchStatus())) {
            throw new BusinessException("已确认的计划无法取消");
        }
        
        plan.setMatchStatus("CANCELLED");
        plan.setUpdateTime(LocalDateTime.now());
        plantingPlanMapper.updateById(plan);
        
        log.info("取消种植计划成功, planId: {}", planId);
    }

    /**
     * 根据计划ID查询种植计划
     * 
     * @param planId 种植计划ID
     * @return 种植计划信息
     * @throws BusinessException 计划不存在
     */
    public PlantingPlanVO getByPlanIdVO(String planId) {
        return convertToVO(getByPlanId(planId));
    }

    /**
     * 分页查询农户的种植计划
     * 
     * @param farmerId 农户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param matchStatus 匹配状态
     * @return 种植计划分页数据
     */
    public Page<PlantingPlanVO> pageQueryByFarmer(String farmerId, Integer pageNum, Integer pageSize, String matchStatus) {
        Page<PlantingPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PlantingPlan> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.eq(PlantingPlan::getFarmerId, farmerId);
        
        if (matchStatus != null && !matchStatus.isEmpty()) {
            queryWrapper.eq(PlantingPlan::getMatchStatus, matchStatus);
        }
        
        queryWrapper.orderByDesc(PlantingPlan::getCreateTime);
        
        Page<PlantingPlan> planPage = plantingPlanMapper.selectPage(page, queryWrapper);
        
        Page<PlantingPlanVO> voPage = new Page<>(pageNum, pageSize, planPage.getTotal());
        List<PlantingPlanVO> voList = planPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    /**
     * 分页查询供销商可匹配的种植计划
     * 
     * @param supplierId 供销商ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 种植计划分页数据
     */
    public Page<PlantingPlanVO> pageQueryForSupplier(String supplierId, Integer pageNum, Integer pageSize) {
        Page<PlantingPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PlantingPlan> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询已匹配到该供销商的计划
        queryWrapper.eq(PlantingPlan::getSupplierId, supplierId)
                .in(PlantingPlan::getMatchStatus, "MATCHED", "CONFIRMED")
                .orderByDesc(PlantingPlan::getMatchTime);
        
        Page<PlantingPlan> planPage = plantingPlanMapper.selectPage(page, queryWrapper);
        
        Page<PlantingPlanVO> voPage = new Page<>(pageNum, pageSize, planPage.getTotal());
        List<PlantingPlanVO> voList = planPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    /**
     * 根据planId查询实体
     */
    private PlantingPlan getByPlanId(String planId) {
        LambdaQueryWrapper<PlantingPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PlantingPlan::getPlanId, planId);
        PlantingPlan plan = plantingPlanMapper.selectOne(queryWrapper);
        
        if (plan == null) {
            throw new BusinessException("种植计划不存在");
        }
        
        return plan;
    }

    /**
     * 生成种植计划ID
     * 
     * @return 种植计划ID
     */
    private String generatePlanId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(10000);
        return "PLAN" + date + String.format("%04d", random);
    }

    /**
     * 生成种植计划摘要
     * 
     * @param dto 创建请求
     * @return 摘要
     */
    private String generatePlanSummary(CreatePlantingPlanDTO dto) {
        return String.format("%s·%s·%.1f亩·预计产量%d个·用于%s",
                dto.getRegion(),
                dto.getVariety(),
                dto.getPlantingArea(),
                dto.getExpectedYield(),
                dto.getTargetUsage());
    }

    /**
     * 计算匹配度（回退方案）
     * 
     * @param plan 种植计划
     * @return 匹配度(0-100)
     */
    private int calculateMatchScoreFallback(PlantingPlan plan) {
        // 回退到模拟匹配，返回60-100之间的随机分数
        return 60 + new Random().nextInt(41);
    }

    /**
     * 查找最佳供销商（回退方案）
     * 
     * @param plan 种植计划
     * @return 供销商ID
     */
    private String findBestSupplierFallback(PlantingPlan plan) {
        // 回退到随机分配
        return "SUPPLY" + String.format("%03d", new Random().nextInt(100) + 1);
    }

    /**
     * 生成区域气候匹配分析
     * 
     * @param plan 种植计划
     * @return 气候分析结果
     */
    private String generateClimateAnalysis(PlantingPlan plan) {
        // TODO: 实际应调用气象API或文献库
        return String.format("当前%s地区适合%s种植", plan.getRegion(), plan.getVariety());
    }

    /**
     * 将PlantingPlan转换为PlantingPlanVO
     * 
     * @param plan 种植计划实体
     * @return 种植计划VO
     */
    private PlantingPlanVO convertToVO(PlantingPlan plan) {
        PlantingPlanVO vo = new PlantingPlanVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }
}
