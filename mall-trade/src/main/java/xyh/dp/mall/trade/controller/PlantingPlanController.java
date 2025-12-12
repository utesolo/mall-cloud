package xyh.dp.mall.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreatePlantingPlanDTO;
import xyh.dp.mall.trade.service.PlantingPlanService;
import xyh.dp.mall.trade.vo.PlantingPlanVO;

/**
 * 种植计划控制器（供给匹配）
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/planting-plan")
@RequiredArgsConstructor
@Tag(name = "种植计划接口", description = "种植计划创建、匹配、查询等接口")
public class PlantingPlanController {

    private final PlantingPlanService plantingPlanService;

    /**
     * 创建种植计划
     * 
     * @param createDTO 创建种植计划请求
     * @return 种植计划信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建种植计划", description = "农户提交种植计划信息")
    public Result<PlantingPlanVO> createPlan(@Valid @RequestBody CreatePlantingPlanDTO createDTO) {
        log.info("创建种植计划请求: farmerId={}, variety={}", createDTO.getFarmerId(), createDTO.getVariety());
        PlantingPlanVO planVO = plantingPlanService.createPlan(createDTO);
        return Result.success(planVO, "种植计划创建成功");
    }

    /**
     * 执行供给匹配
     * 
     * @param planId 种植计划ID
     * @return 匹配后的种植计划信息
     */
    @PostMapping("/{planId}/match")
    @Operation(summary = "执行供给匹配", description = "对种植计划执行AI供给匹配")
    public Result<PlantingPlanVO> executeMatch(@Parameter(description = "种植计划ID") @PathVariable String planId) {
        log.info("执行供给匹配请求: planId={}", planId);
        PlantingPlanVO planVO = plantingPlanService.executeMatch(planId);
        return Result.success(planVO, "供给匹配成功");
    }

    /**
     * 确认匹配结果
     * 
     * @param planId 种植计划ID
     * @param farmerId 农户ID
     * @return 操作结果
     */
    @PostMapping("/{planId}/confirm")
    @Operation(summary = "确认匹配", description = "农户确认匹配结果")
    public Result<Void> confirmMatch(
            @Parameter(description = "种植计划ID") @PathVariable String planId,
            @Parameter(description = "农户ID") @RequestParam String farmerId
    ) {
        log.info("确认匹配请求: planId={}, farmerId={}", planId, farmerId);
        plantingPlanService.confirmMatch(planId, farmerId);
        return Result.success(null, "匹配确认成功");
    }

    /**
     * 取消种植计划
     * 
     * @param planId 种植计划ID
     * @param farmerId 农户ID
     * @return 操作结果
     */
    @PostMapping("/{planId}/cancel")
    @Operation(summary = "取消种植计划", description = "取消未确认的种植计划")
    public Result<Void> cancelPlan(
            @Parameter(description = "种植计划ID") @PathVariable String planId,
            @Parameter(description = "农户ID") @RequestParam String farmerId
    ) {
        log.info("取消种植计划请求: planId={}, farmerId={}", planId, farmerId);
        plantingPlanService.cancelPlan(planId, farmerId);
        return Result.success(null, "种植计划取消成功");
    }

    /**
     * 查询种植计划详情
     * 
     * @param planId 种植计划ID
     * @return 种植计划详情
     */
    @GetMapping("/{planId}")
    @Operation(summary = "查询种植计划详情", description = "根据种植计划ID查询详细信息")
    public Result<PlantingPlanVO> getByPlanId(@Parameter(description = "种植计划ID") @PathVariable String planId) {
        PlantingPlanVO planVO = plantingPlanService.getByPlanIdVO(planId);
        return Result.success(planVO);
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
    @GetMapping("/farmer/page")
    @Operation(summary = "分页查询农户种植计划", description = "查询指定农户的种植计划列表")
    public Result<Page<PlantingPlanVO>> pageQueryByFarmer(
            @Parameter(description = "农户ID") @RequestParam String farmerId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "匹配状态") @RequestParam(required = false) String matchStatus
    ) {
        Page<PlantingPlanVO> page = plantingPlanService.pageQueryByFarmer(farmerId, pageNum, pageSize, matchStatus);
        return Result.success(page);
    }

    /**
     * 分页查询供销商可匹配的种植计划
     * 
     * @param supplierId 供销商ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 种植计划分页数据
     */
    @GetMapping("/supplier/page")
    @Operation(summary = "分页查询供销商匹配计划", description = "查询匹配到指定供销商的种植计划")
    public Result<Page<PlantingPlanVO>> pageQueryForSupplier(
            @Parameter(description = "供销商ID") @RequestParam String supplierId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<PlantingPlanVO> page = plantingPlanService.pageQueryForSupplier(supplierId, pageNum, pageSize);
        return Result.success(page);
    }
}
