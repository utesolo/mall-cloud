package xyh.dp.mall.trade.tracking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.tracking.dto.TrackingEventDTO;
import xyh.dp.mall.trade.tracking.service.TrackingService;
import xyh.dp.mall.trade.tracking.service.TrainingDataExportService;

import java.time.LocalDateTime;

/**
 * 埋点与训练数据控制器
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Tag(name = "埋点与训练数据接口", description = "用户行为埋点收集与训练数据导出")
public class TrackingController {

    private final TrackingService trackingService;
    private final TrainingDataExportService exportService;

    /**
     * 上报埋点事件
     * 
     * @param dto 埋点事件数据
     * @return 操作结果
     */
    @PostMapping("/event")
    @Operation(summary = "上报埋点事件", description = "接收前端上报的用户行为埋点数据")
    public Result<Void> trackEvent(@RequestBody TrackingEventDTO dto) {
        log.info("收到埋点事件: eventType={}, planId={}, productId={}", 
                dto.getEventType(), dto.getPlanId(), dto.getProductId());
        trackingService.trackEvent(dto);
        return Result.success(null, "埋点记录成功");
    }

    /**
     * 批量上报埋点事件
     * 
     * @param dtos 埋点事件数据列表
     * @return 操作结果
     */
    @PostMapping("/events/batch")
    @Operation(summary = "批量上报埋点事件", description = "接收前端批量上报的用户行为埋点数据")
    public Result<Integer> trackEvents(@RequestBody java.util.List<TrackingEventDTO> dtos) {
        log.info("收到批量埋点事件, 数量: {}", dtos.size());
        for (TrackingEventDTO dto : dtos) {
            trackingService.trackEvent(dto);
        }
        return Result.success(dtos.size(), "批量埋点记录成功");
    }

    /**
     * 导出训练数据CSV
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return CSV文件
     */
    @GetMapping("/export/training-data")
    @Operation(summary = "导出训练数据CSV", description = "导出指定时间范围内的所有埋点数据用于模型训练")
    public ResponseEntity<byte[]> exportTrainingData(
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            byte[] csvData = exportService.exportTrainingDataAsCsv(startTime, endTime);
            String fileName = exportService.generateFileName("training_data");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvData);
        } catch (Exception e) {
            log.error("导出训练数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出正样本数据CSV
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return CSV文件
     */
    @GetMapping("/export/positive-samples")
    @Operation(summary = "导出正样本数据CSV", description = "导出指定时间范围内的正样本（确认/购买）埋点数据")
    public ResponseEntity<byte[]> exportPositiveSamples(
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            byte[] csvData = exportService.exportPositiveSamplesAsCsv(startTime, endTime);
            String fileName = exportService.generateFileName("positive_samples");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvData);
        } catch (Exception e) {
            log.error("导出正样本数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取训练数据统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取训练数据统计", description = "获取指定时间范围内的埋点数据统计信息")
    public Result<TrainingDataExportService.TrainingDataStats> getStats(
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        TrainingDataExportService.TrainingDataStats stats = exportService.getStats(startTime, endTime);
        return Result.success(stats, "获取统计信息成功");
    }
}
