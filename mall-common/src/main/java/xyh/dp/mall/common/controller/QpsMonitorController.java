package xyh.dp.mall.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.monitor.QpsMonitor;
import xyh.dp.mall.common.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * QPS监控管理接口
 * 提供QPS查询和重置功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@RestController
@RequestMapping("/monitor/qps")
@RequiredArgsConstructor
@Tag(name = "QPS监控", description = "QPS监控查询和管理接口")
public class QpsMonitorController {

    private final QpsMonitor qpsMonitor;

    /**
     * 获取指定接口的QPS信息
     * 
     * @param endpoint 接口端点
     * @return QPS信息（当前QPS、峰值QPS）
     */
    @GetMapping("/query")
    @Operation(summary = "查询QPS", description = "查询指定接口的当前QPS和峰值QPS")
    public Result<Map<String, Object>> getQps(
            @Parameter(description = "接口端点，如：/product/page") 
            @RequestParam String endpoint) {
        
        long currentQps = qpsMonitor.getCurrentQps(endpoint);
        long peakQps = qpsMonitor.getPeakQps(endpoint);
        
        Map<String, Object> data = new HashMap<>();
        data.put("endpoint", endpoint);
        data.put("currentQps", currentQps);
        data.put("peakQps", peakQps);
        data.put("timestamp", System.currentTimeMillis());
        
        return Result.success(data);
    }

    /**
     * 重置指定接口的峰值QPS
     * 
     * @param endpoint 接口端点
     * @return 操作结果
     */
    @PostMapping("/reset-peak")
    @Operation(summary = "重置峰值QPS", description = "重置指定接口的峰值QPS记录")
    public Result<Void> resetPeakQps(
            @Parameter(description = "接口端点，如：/product/page") 
            @RequestParam String endpoint) {
        
        qpsMonitor.resetPeakQps(endpoint);
        return Result.success();
    }

    /**
     * 批量查询多个接口的QPS信息
     * 
     * @param endpoints 接口端点列表（逗号分隔）
     * @return QPS信息列表
     */
    @GetMapping("/batch-query")
    @Operation(summary = "批量查询QPS", description = "批量查询多个接口的QPS信息")
    public Result<Map<String, Map<String, Object>>> batchQuery(
            @Parameter(description = "接口端点列表，逗号分隔") 
            @RequestParam String endpoints) {
        
        Map<String, Map<String, Object>> result = new HashMap<>();
        String[] endpointArray = endpoints.split(",");
        
        for (String endpoint : endpointArray) {
            endpoint = endpoint.trim();
            if (!endpoint.isEmpty()) {
                Map<String, Object> qpsInfo = new HashMap<>();
                qpsInfo.put("currentQps", qpsMonitor.getCurrentQps(endpoint));
                qpsInfo.put("peakQps", qpsMonitor.getPeakQps(endpoint));
                result.put(endpoint, qpsInfo);
            }
        }
        
        return Result.success(result);
    }
}
