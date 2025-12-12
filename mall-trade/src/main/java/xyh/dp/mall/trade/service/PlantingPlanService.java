package xyh.dp.mall.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.trade.dto.CreatePlantingPlanDTO;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.mapper.PlantingPlanMapper;
import xyh.dp.mall.trade.vo.PlantingPlanVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 执行供给匹配（模拟AI算法匹配）
     * 
     * @param planId 种植计划ID
     * @return 匹配后的种植计划信息
     * @throws BusinessException 计划不存在或已匹配
     */
    @Transactional(rollbackFor = Exception.class)
    public PlantingPlanVO executeMatch(String planId) {
        PlantingPlan plan = getByPlanId(planId);
        
        if (!"PENDING".equals(plan.getMatchStatus())) {
            throw new BusinessException("该计划已匹配或已取消");
        }
        
        // 模拟AI匹配算法
        int matchScore = calculateMatchScore(plan);
        String supplierId = findBestSupplier(plan);
        String climateMatch = generateClimateAnalysis(plan);
        
        // 更新匹配信息
        plan.setMatchScore(matchScore);
        plan.setSupplierId(supplierId);
        plan.setClimateMatch(climateMatch);
        plan.setMatchTime(LocalDateTime.now());
        plan.setMatchStatus("MATCHED");
        plan.setUpdateTime(LocalDateTime.now());
        
        plantingPlanMapper.updateById(plan);
        log.info("供给匹配成功, planId: {}, supplierId: {}, matchScore: {}", planId, supplierId, matchScore);
        
        return convertToVO(plan);
    }

    /**
     * 确认匹配结果
     * 
     * @param planId 种植计划ID
     * @param farmerId 农户ID
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
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
        
        log.info("确认匹配成功, planId: {}", planId);
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
     * 计算匹配度（模拟AI算法）
     * 
     * @param plan 种植计划
     * @return 匹配度(0-100)
     */
    private int calculateMatchScore(PlantingPlan plan) {
        // TODO: 实际应调用AI模型，基于天气、历史记录、区域等计算
        // 模拟返回60-100之间的随机分数
        return 60 + new Random().nextInt(41);
    }

    /**
     * 查找最佳供销商（模拟）
     * 
     * @param plan 种植计划
     * @return 供销商ID
     */
    private String findBestSupplier(PlantingPlan plan) {
        // TODO: 实际应根据品种、区域、历史合作等匹配供销商
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
