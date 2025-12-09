package xyh.dp.mall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品服务启动类
 * 管理商品、分类、品牌等功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MallProductApplication {

    /**
     * 启动商品服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }
}
