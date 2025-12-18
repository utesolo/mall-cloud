package xyh.dp.mall.trade.matching.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 匹配服务线程池配置
 * 异步匹配任务的专用线程池
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class MatchingThreadPoolConfig {

    /**
     * 匹配任务核心线程数
     */
    private static final int CORE_POOL_SIZE = 4;

    /**
     * 匹配任务最大线程数
     */
    private static final int MAX_POOL_SIZE = 10;

    /**
     * 待处理任务队列容量
     */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 线程保活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "match-async-";

    /**
     * 创建匹配任务专用线程池执行器
     *
     * @return 异步匹配执行器
     */
    @Bean(name = "matchingTaskExecutor")
    public Executor matchingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心配置
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        
        // 拒绝策略: CallerRunsPolicy确保任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 初始化执行器
        executor.initialize();
        
        log.info("匹配线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
        return executor;
    }
}
