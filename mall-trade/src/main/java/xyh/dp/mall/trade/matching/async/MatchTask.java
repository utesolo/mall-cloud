package xyh.dp.mall.trade.matching.async;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异步匹配任务实体
 * 存储匹配任务的状态和进度
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
public class MatchTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 种植计划ID
     */
    private String planId;

    /**
     * 农户ID
     */
    private String farmerId;

    /**
     * 任务状态: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    private TaskStatus status;

    /**
     * 匹配进度 (0-100)
     */
    private Integer progress;

    /**
     * 待匹配商品总数
     */
    private Integer totalProducts;

    /**
     * 已匹配商品数
     */
    private Integer matchedProducts;

    /**
     * 匹配结果 (Top-5商品)
     */
    private MatchResult result;

    /**
     * 失败时的错误信息
     */
    private String errorMessage;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 匹配开始时间
     */
    private LocalDateTime startTime;

    /**
     * 匹配结束时间
     */
    private LocalDateTime endTime;

    /**
     * 任务过期时间 (用于Redis TTL)
     */
    private LocalDateTime expireTime;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 等待队列中
         */
        PENDING,

        /**
         * 正在处理
         */
        PROCESSING,

        /**
         * 匹配成功完成
         */
        COMPLETED,

        /**
         * 匹配失败
         */
        FAILED,

        /**
         * 用户取消
         */
        CANCELLED
    }

    /**
     * 创建新的待处理任务
     *
     * @param taskId   任务ID
     * @param planId   种植计划ID
     * @param farmerId 农户ID
     * @return 新的MatchTask实例
     */
    public static MatchTask createPending(String taskId, String planId, String farmerId) {
        MatchTask task = new MatchTask();
        task.setTaskId(taskId);
        task.setPlanId(planId);
        task.setFarmerId(farmerId);
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setTotalProducts(0);
        task.setMatchedProducts(0);
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusHours(24));
        return task;
    }

    /**
     * 更新任务为处理中状态
     *
     * @param totalProducts 待匹配商品总数
     */
    public void startProcessing(int totalProducts) {
        this.status = TaskStatus.PROCESSING;
        this.startTime = LocalDateTime.now();
        this.totalProducts = totalProducts;
        this.matchedProducts = 0;
        this.progress = 0;
    }

    /**
     * 更新匹配进度
     *
     * @param matchedCount 已匹配商品数
     */
    public void updateProgress(int matchedCount) {
        this.matchedProducts = matchedCount;
        if (totalProducts > 0) {
            this.progress = (int) ((matchedCount * 100.0) / totalProducts);
        }
    }

    /**
     * 标记任务为完成
     *
     * @param result 匹配结果
     */
    public void complete(MatchResult result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.progress = 100;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 标记任务为失败
     *
     * @param errorMessage 错误信息
     */
    public void fail(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 检查任务是否已结束（完成、失败或取消）
     *
     * @return 如果任务已结束返回true
     */
    public boolean isFinished() {
        return status == TaskStatus.COMPLETED
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED;
    }
}
