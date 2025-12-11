package xyh.dp.mall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 认证授权服务启动类
 * 提供微信登录、JWT签发等认证授权功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"xyh.dp.mall.auth", "xyh.dp.mall.common"})
public class MallAuthApplication {

    /**
     * 启动认证授权服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }
}
