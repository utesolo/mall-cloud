package xyh.dp.mall.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务服务启动类
 * 处理过期订单、数据统计等定时任务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class MallJobApplication {

    /**
     * 启动定时任务服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallJobApplication.class, args);
    }
}
