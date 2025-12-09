package xyh.dp.mall.zipkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 链路追踪服务启动类
 * 基于Zipkin提供分布式链路追踪功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MallZipkinApplication {

    /**
     * 启动链路追踪服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallZipkinApplication.class, args);
    }
}
