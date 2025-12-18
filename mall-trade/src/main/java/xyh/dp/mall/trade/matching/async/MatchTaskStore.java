package xyh.dp.mall.trade.matching.async;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 匹配任务存储服务
 * 使用Redis进行持久化存储，并提供本地缓存回退
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchTaskStore {

    private static final String TASK_KEY_PREFIX = "match:task:";
    private static final String QUEUE_KEY = "match:queue";
    private static final Duration TASK_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis不可用时的本地缓存回退
     */
    private final ConcurrentHashMap<String, MatchTask> localCache = new ConcurrentHashMap<>();

    /**
     * 生成唯一任务ID
     *
     * @return 任务ID
     */
    public String generateTaskId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return "MATCH" + timestamp + String.format("%04d", random);
    }

    /**
     * 保存匹配任务
     *
     * @param task 要保存的匹配任务
     */
    public void saveTask(MatchTask task) {
        String key = TASK_KEY_PREFIX + task.getTaskId();
        try {
            String json = JSON.toJSONString(task);
            redisTemplate.opsForValue().set(key, json, TASK_TTL);
            log.debug("Saved match task to Redis: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.warn("Failed to save task to Redis, using local cache: {}", e.getMessage());
            localCache.put(task.getTaskId(), task);
        }
    }

    /**
     * 根据ID获取匹配任务
     *
     * @param taskId 任务ID
     * @return 匹配任务，不存在返回null
     */
    public MatchTask getTask(String taskId) {
        String key = TASK_KEY_PREFIX + taskId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return JSON.parseObject(json, MatchTask.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get task from Redis, checking local cache: {}", e.getMessage());
        }
        return localCache.get(taskId);
    }

    /**
     * 更新匹配任务
     *
     * @param task 要更新的匹配任务
     */
    public void updateTask(MatchTask task) {
        saveTask(task);
    }

    /**
     * 删除匹配任务
     *
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        String key = TASK_KEY_PREFIX + taskId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete task from Redis: {}", e.getMessage());
        }
        localCache.remove(taskId);
    }

    /**
     * 将任务加入处理队列
     *
     * @param taskId 任务ID
     */
    public void enqueue(String taskId) {
        try {
            redisTemplate.opsForList().rightPush(QUEUE_KEY, taskId);
            log.debug("Task enqueued: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("Failed to enqueue task: {}", e.getMessage());
        }
    }

    /**
     * 从队列获取下一个任务（非阻塞）
     *
     * @return 任务ID，队列为空返回null
     */
    public String dequeue() {
        try {
            return redisTemplate.opsForList().leftPop(QUEUE_KEY);
        } catch (Exception e) {
            log.warn("Failed to dequeue task: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取队列大小
     *
     * @return 队列大小
     */
    public long getQueueSize() {
        try {
            Long size = redisTemplate.opsForList().size(QUEUE_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.warn("Failed to get queue size: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 存在返回true
     */
    public boolean exists(String taskId) {
        return getTask(taskId) != null;
    }

    /**
     * 获取任务在队列中的位置
     *
     * @param taskId 任务ID
     * @return 位置（0开始），不在队列中返回-1
     */
    public long getQueuePosition(String taskId) {
        try {
            Long size = redisTemplate.opsForList().size(QUEUE_KEY);
            if (size == null || size == 0) {
                return -1;
            }
            for (int i = 0; i < size; i++) {
                String queuedTaskId = redisTemplate.opsForList().index(QUEUE_KEY, i);
                if (taskId.equals(queuedTaskId)) {
                    return i;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get queue position: {}", e.getMessage());
        }
        return -1;
    }
}
