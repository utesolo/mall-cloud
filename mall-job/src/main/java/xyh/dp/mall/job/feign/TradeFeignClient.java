package xyh.dp.mall.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import xyh.dp.mall.common.result.Result;

import java.util.List;

/**
 * 交易服务Feign客户端
 * 用于调用mall-trade服务的接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@FeignClient(name = "mall-trade", fallbackFactory = TradeFeignFallback.class)
public interface TradeFeignClient {

    /**
     * 查询一周内的热销商品排行（前N）
     * 
     * @param topN 返回前N个商品
     * @return 热销商品列表
     */
    @GetMapping("/hot-product/weekly/top/{topN}")
    Result<List<Object>> getWeeklyHotProducts(@PathVariable("topN") Integer topN);
}
