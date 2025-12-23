package xyh.dp.mall.trade.matching.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.entity.PlantingPlan;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.PlantingPlanMapper;
import xyh.dp.mall.trade.matching.engine.MatchScoreCalculator;
import xyh.dp.mall.trade.matching.engine.MLHybridMatchService;
import xyh.dp.mall.trade.matching.feature.MatchFeature;

import java.util.ArrayList;
import java.util.List;

/**
 * 异步匹配服务
 * 处理种植计划与商品的异步匹配
 * 返回每个计划的Top-5最佳匹配商品
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMatchService {

    /**
     * 默认返回的最佳结果数量
     */
    private static final int TOP_N = 5;

    /**
     * 每次匹配任务最大评估商品数
     */
    private static final int MAX_PRODUCTS_TO_EVALUATE = 100;

    private final MatchTaskStore taskStore;
    private final PlantingPlanMapper planMapper;
    private final ProductFeignClient productFeignClient;
    private final MatchScoreCalculator scoreCalculator;
    private final MLHybridMatchService mlHybridMatchService;

    /**
     * 提交新的匹配任务
     * 立即返回任务ID，匹配异步运行
     *
     * @param planId   种植计划ID
     * @param farmerId 农户ID（用于授权验证）
     * @return 任务ID，用于查询状态
     * @throws BusinessException 计划不存在或已有待处理任务时抛出
     */
    public String submitMatchTask(String planId, String farmerId) {
        log.info("Submitting match task: planId={}, farmerId={}", planId, farmerId);

        // Validate plan exists
        PlantingPlan plan = getPlanByPlanId(planId);
        if (plan == null) {
            throw new BusinessException("Planting plan not found");
        }

        // Validate ownership
        if (!plan.getFarmerId().equals(farmerId)) {
            throw new BusinessException("Not authorized to access this plan");
        }

        // Create task
        String taskId = taskStore.generateTaskId();
        MatchTask task = MatchTask.createPending(taskId, planId, farmerId);
        taskStore.saveTask(task);
        taskStore.enqueue(taskId);

        log.info("Match task submitted: taskId={}, planId={}", taskId, planId);

        // Trigger async processing
        processMatchTaskAsync(taskId);

        return taskId;
    }

    /**
     * 查询匹配任务状态
     *
     * @param taskId 任务ID
     * @return 匹配任务及当前状态
     * @throws BusinessException 任务不存在时抛出
     */
    public MatchTask getTaskStatus(String taskId) {
        MatchTask task = taskStore.getTask(taskId);
        if (task == null) {
            throw new BusinessException("Match task not found");
        }

        // Add queue position if still pending
        if (task.getStatus() == MatchTask.TaskStatus.PENDING) {
            long position = taskStore.getQueuePosition(taskId);
            if (position >= 0) {
                log.debug("Task {} is at queue position {}", taskId, position);
            }
        }

        return task;
    }

    /**
     * 取消匹配任务
     *
     * @param taskId   任务ID
     * @param farmerId 农户ID（用于授权验证）
     * @throws BusinessException 任务不存在或无权操作时抛出
     */
    public void cancelTask(String taskId, String farmerId) {
        MatchTask task = taskStore.getTask(taskId);
        if (task == null) {
            throw new BusinessException("Match task not found");
        }

        if (!task.getFarmerId().equals(farmerId)) {
            throw new BusinessException("Not authorized to cancel this task");
        }

        if (task.isFinished()) {
            throw new BusinessException("Task already finished, cannot cancel");
        }

        task.setStatus(MatchTask.TaskStatus.CANCELLED);
        taskStore.updateTask(task);
        log.info("Match task cancelled: taskId={}", taskId);
    }

    /**
     * 获取队列信息
     *
     * @return 当前队列大小
     */
    public long getQueueSize() {
        return taskStore.getQueueSize();
    }

    /**
     * 异步处理匹配任务
     * 使用专用线程池执行匹配操作
     *
     * @param taskId 要处理的任务ID
     */
    @Async("matchingTaskExecutor")
    public void processMatchTaskAsync(String taskId) {
        log.info("Starting async matching: taskId={}", taskId);
        long startTime = System.currentTimeMillis();

        MatchTask task = taskStore.getTask(taskId);
        if (task == null) {
            log.warn("Task not found for processing: taskId={}", taskId);
            return;
        }

        // Check if task was cancelled
        if (task.getStatus() == MatchTask.TaskStatus.CANCELLED) {
            log.info("Task was cancelled, skipping: taskId={}", taskId);
            return;
        }

        try {
            // Get planting plan
            PlantingPlan plan = getPlanByPlanId(task.getPlanId());
            if (plan == null) {
                task.fail("Planting plan not found");
                taskStore.updateTask(task);
                return;
            }

            // Get candidate products
            List<ProductDTO> candidates = getCandidateProducts(plan);
            if (candidates.isEmpty()) {
                task.fail("No candidate products found");
                taskStore.updateTask(task);
                return;
            }

            // Start processing
            task.startProcessing(candidates.size());
            taskStore.updateTask(task);

            // Calculate scores for all candidates
            List<MatchFeature> features = new ArrayList<>();
            int processed = 0;

            for (ProductDTO product : candidates) {
                // 检查处理过程中任务是否被取消
                MatchTask currentTask = taskStore.getTask(taskId);
                if (currentTask != null && currentTask.getStatus() == MatchTask.TaskStatus.CANCELLED) {
                    log.info("Task cancelled during processing: taskId={}", taskId);
                    return;
                }

                try {
                    MatchFeature feature = mlHybridMatchService.calculateScore(plan, product);
                    features.add(feature);
                } catch (Exception e) {
                    log.warn("Failed to calculate score for product {}: {}", product.getId(), e.getMessage());
                }

                processed++;
                task.updateProgress(processed);

                // Update progress every 10 products
                if (processed % 10 == 0) {
                    taskStore.updateTask(task);
                }
            }

            // Build result with Top-5
            long durationMs = System.currentTimeMillis() - startTime;
            MatchResult result = MatchResult.fromFeatures(
                    task.getPlanId(),
                    features,
                    TOP_N,
                    candidates.size(),
                    durationMs
            );

            // Set ranking positions
            for (int i = 0; i < result.getTopMatches().size(); i++) {
                result.getTopMatches().get(i).setRank(i + 1);
            }

            // Complete task
            task.complete(result);
            taskStore.updateTask(task);

            log.info("Match task completed: taskId={}, matchCount={}, duration={}ms",
                    taskId, result.getMatchCount(), durationMs);

        } catch (Exception e) {
            log.error("Match task failed: taskId={}, error={}", taskId, e.getMessage(), e);
            task.fail("Matching failed: " + e.getMessage());
            taskStore.updateTask(task);
        }
    }

    /**
     * 根据计划ID获取种植计划
     *
     * @param planId 计划ID
     * @return 种植计划或null
     */
    private PlantingPlan getPlanByPlanId(String planId) {
        return planMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlantingPlan>()
                        .eq(PlantingPlan::getPlanId, planId)
        );
    }

    /**
     * 获取匹配候选商品
     *
     * @param plan 种植计划
     * @return 候选商品列表
     */
    private List<ProductDTO> getCandidateProducts(PlantingPlan plan) {
        try {
            // Search products by variety and region
            Result<List<ProductDTO>> result = productFeignClient.searchProducts(
                    plan.getVariety(),
                    plan.getRegion(),
                    MAX_PRODUCTS_TO_EVALUATE
            );

            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.error("Failed to get candidate products: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}
