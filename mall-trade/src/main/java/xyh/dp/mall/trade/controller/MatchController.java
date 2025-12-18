package xyh.dp.mall.trade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyh.dp.mall.common.annotation.RequireLogin;
import xyh.dp.mall.common.context.UserContextHolder;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.matching.async.AsyncMatchService;
import xyh.dp.mall.trade.matching.async.MatchResult;
import xyh.dp.mall.trade.matching.async.MatchTask;

/**
 * 匹配控制器
 * 提供异步匹配 API 接口
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
@Tag(name = "异步匹配接口", description = "种植计划与商品的异步匹配")
public class MatchController {

    private final AsyncMatchService asyncMatchService;

    /**
     * 提交匹配任务
     * 立即返回任务ID，匹配异步执行
     *
     * @param planId 种植计划ID
     * @return 任务ID
     */
    @PostMapping("/submit/{planId}")
    @RequireLogin(allowedTypes = "FARMER")
    @Operation(summary = "提交匹配任务", description = "提交异步匹配，立即返回任务ID")
    public Result<MatchTaskResponse> submitMatchTask(
            @Parameter(description = "Planting plan ID") @PathVariable String planId
    ) {
        String farmerId = UserContextHolder.getBusinessUserId();
        log.info("Submit match task request: planId={}, farmerId={}", planId, farmerId);

        String taskId = asyncMatchService.submitMatchTask(planId, farmerId);

        MatchTaskResponse response = new MatchTaskResponse();
        response.setTaskId(taskId);
        response.setStatus("PENDING");
        response.setMessage("Matching task submitted, please poll for status");
        response.setQueuePosition(asyncMatchService.getQueueSize());

        return Result.success(response, "Matching task submitted");
    }

    /**
     * 查询匹配任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态，完成时包含结果
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询任务状态", description = "轮询匹配任务状态，完成时获取结果")
    public Result<MatchTask> getTaskStatus(
            @Parameter(description = "Task ID") @PathVariable String taskId
    ) {
        log.debug("Get task status: taskId={}", taskId);
        MatchTask task = asyncMatchService.getTaskStatus(taskId);
        return Result.success(task);
    }

    /**
     * 获取匹配结果
     *
     * @param taskId 任务ID
     * @return Top-5匹配商品结果
     */
    @GetMapping("/result/{taskId}")
    @Operation(summary = "获取匹配结果", description = "任务完成时获取Top-5匹配商品")
    public Result<MatchResult> getMatchResult(
            @Parameter(description = "Task ID") @PathVariable String taskId
    ) {
        log.info("Get match result: taskId={}", taskId);

        MatchTask task = asyncMatchService.getTaskStatus(taskId);

        if (task.getStatus() != MatchTask.TaskStatus.COMPLETED) {
            return Result.fail("Task not completed yet, current status: " + task.getStatus());
        }

        return Result.success(task.getResult());
    }

    /**
     * 取消匹配任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{taskId}")
    @RequireLogin(allowedTypes = "FARMER")
    @Operation(summary = "取消任务", description = "取消等待中或处理中的匹配任务")
    public Result<Void> cancelTask(
            @Parameter(description = "Task ID") @PathVariable String taskId
    ) {
        String farmerId = UserContextHolder.getBusinessUserId();
        log.info("Cancel task request: taskId={}, farmerId={}", taskId, farmerId);

        asyncMatchService.cancelTask(taskId, farmerId);
        return Result.success(null, "Task cancelled");
    }

    /**
     * 获取队列状态
     *
     * @return 队列状态信息
     */
    @GetMapping("/queue/status")
    @Operation(summary = "获取队列状态", description = "查询当前匹配队列状态")
    public Result<QueueStatusResponse> getQueueStatus() {
        QueueStatusResponse response = new QueueStatusResponse();
        response.setQueueSize(asyncMatchService.getQueueSize());
        return Result.success(response);
    }

    /**
     * 任务提交响应DTO
     */
    @lombok.Data
    public static class MatchTaskResponse {
        private String taskId;
        private String status;
        private String message;
        private Long queuePosition;
    }

    /**
     * 队列状态响应DTO
     */
    @lombok.Data
    public static class QueueStatusResponse {
        private Long queueSize;
    }
}
