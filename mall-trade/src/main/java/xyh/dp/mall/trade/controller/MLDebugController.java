package xyh.dp.mall.trade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.matching.engine.MLHybridMatchService;

/**
 * ML模型调试控制器
 * 用于查看和管理ML模型的状态
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/ml")
@RequiredArgsConstructor
@Tag(name = "ML模型调试", description = "查看和管理机器学习模型的状态")
public class MLDebugController {

    private final MLHybridMatchService mlHybridMatchService;

    /**
     * 获取ML模型配置信息
     *
     * @return ML模型配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取ML模型配置", description = "查看当前ML模型的启用状态、API地址和流量比例")
    public Result<MLHybridMatchService.MLModelConfig> getMLConfig() {
        try {
            MLHybridMatchService.MLModelConfig config = mlHybridMatchService.getModelConfig();
            log.info("获取ML模型配置: {}", config);
            return Result.success(config, "获取ML模型配置成功");
        } catch (Exception e) {
            log.error("获取ML模型配置失败: {}", e.getMessage(), e);
            return Result.error("获取ML模型配置失败");
        }
    }

    /**
     * 获取模型使用说明
     *
     * @return 使用说明
     */
    @GetMapping("/usage")
    @Operation(summary = "获取使用说明", description = "获取ML模型的使用说明和配置指南")
    public Result<String> getUsageGuide() {
        String guide = "ML模型混合匹配说明\n" +
                "\n1. 配置项说明\n" +
                "   - ml.model.enabled: 是否启用ML模型 (true/false)\n" +
                "   - ml.model.api-url: ML模型API地址\n" +
                "   - ml.model.traffic-ratio: ML模型流量比例 (0-100)\n" +
                "\n2. 流量比例含义\n" +
                "   - 0: 100%规则引擎，0%ML模型\n" +
                "   - 50: 50%规则引擎，50%ML模型\n" +
                "   - 100: 0%规则引擎，100%ML模型\n" +
                "\n3. 渐进式切换计划\n" +
                "   - 第1-2周: traffic-ratio=0 (100%规则引擎，收集数据)\n" +
                "   - 第3-4周: traffic-ratio=5-10 (试验ML模型)\n" +
                "   - 第5-6周: traffic-ratio=20-50 (逐步提升)\n" +
                "   - 第7周+: traffic-ratio=100 (完全切换)\n" +
                "\n4. 降级保障\n" +
                "   - 如果ML API不可用，自动降级到规则引擎\n" +
                "   - 规则引擎始终保留，确保系统稳定\n" +
                "\n5. 配置修改\n" +
                "   在 application.yml 中修改配置项，然后重启服务生效";

        return Result.success(guide, "获取使用说明成功");
    }

    /**
     * 测试ML模型连接（健康检查）
     *
     * @return 连接测试结果
     */
    @GetMapping("/test-connection")
    @Operation(summary = "测试ML API连接", description = "测试与ML模型API的连接是否正常")
    public Result<String> testMLConnection() {
        try {
            MLHybridMatchService.MLModelConfig config = mlHybridMatchService.getModelConfig();
            
            if (!config.isMlEnabled()) {
                return Result.error("ML模型已禁用，无法测试连接");
            }
            
            log.info("测试ML API连接: {}", config.getMlApiUrl());
            
            // 实际的连接测试可以在MLHybridMatchService中实现
            // 这里仅返回配置状态
            return Result.success(
                    "ML API配置: " + config.getMlApiUrl() + "\n" +
                    "流量比例: " + config.getMlTrafficRatio() + "%\n" +
                    "注意: 实际连接测试需要启动Python ML服务",
                    "ML模型配置检查完成"
            );
        } catch (Exception e) {
            log.error("测试ML连接失败: {}", e.getMessage(), e);
            return Result.error("测试ML连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置调整建议
     *
     * @return 建议信息
     */
    @GetMapping("/suggestions")
    @Operation(summary = "获取配置调整建议", description = "基于当前状态给出配置调整建议")
    public Result<String> getSuggestions() {
        MLHybridMatchService.MLModelConfig config = mlHybridMatchService.getModelConfig();
        
        StringBuilder suggestions = new StringBuilder("配置调整建议\n\n");
        
        if (!config.isMlEnabled()) {
            suggestions.append("✓ ML模型已禁用\n")
                    .append("  建议: 数据收集阶段完成后，在application.yml中设置 ml.model.enabled=true\n\n");
        } else {
            suggestions.append("✓ ML模型已启用\n");
            
            if (config.getMlTrafficRatio() == 0) {
                suggestions.append("  当前状态: 100%规则引擎，0%ML模型\n")
                        .append("  建议: 继续收集数据，等待积累充足训练样本\n")
                        .append("  预计时间: 1-2周内\n\n");
            } else if (config.getMlTrafficRatio() < 10) {
                suggestions.append("  当前状态: 试验阶段\n")
                        .append("  建议: 监控ML模型预测结果，观察准确率\n")
                        .append("  如效果满意，可继续提升流量比例\n\n");
            } else if (config.getMlTrafficRatio() < 100) {
                suggestions.append("  当前状态: 灰度发布阶段\n")
                        .append("  建议: 持续监控，逐步提升流量比例\n")
                        .append("  可设置: 20% -> 50% -> 80% -> 100%\n\n");
            } else {
                suggestions.append("  当前状态: 全量使用ML模型\n")
                        .append("  建议: 定期离线重新训练模型，持续优化效果\n\n");
            }
        }
        
        suggestions.append("启动Python ML服务\n")
                .append("1. 进入ml-model目录\n")
                .append("2. 运行: python predict_api.py ./models 5000\n")
                .append("3. 或指定其他端口: python predict_api.py ./models 8000\n")
                .append("4. 相应修改配置: ml.model.api-url=http://localhost:8000\n");
        
        return Result.success(suggestions.toString(), "获取建议成功");
    }
}
