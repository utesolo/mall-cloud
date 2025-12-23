package xyh.dp.mall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 搜索服务启动类
 * 提供基于Elasticsearch的商品搜索功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class MallSearchApplication {

    /**
     * 启动搜索服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallSearchApplication.class, args);
    }
}
