package xyh.dp.mall.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 交易服务启动类
 * 提供购物车、订单、支付回调等交易功能
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "xyh.dp.mall.trade.feign")
@ComponentScan(basePackages = {"xyh.dp.mall.trade", "xyh.dp.mall.common"})
public class MallTradeApplication {

    /**
     * 启动交易服务
     * 
     * @param args 启动参数
     */
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SpringApplication.run(MallTradeApplication.class, args);
    }
}
