package xyh.dp.mall.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务服务启动类
 * 处理ES数据同步、热销榜单更新、订单超时等定时任务
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"xyh.dp.mall.job", "xyh.dp.mall.common"})
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
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
