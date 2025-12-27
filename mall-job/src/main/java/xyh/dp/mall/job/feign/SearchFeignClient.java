package xyh.dp.mall.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import xyh.dp.mall.common.result.Result;

import java.util.Map;

/**
 * 搜索服务Feign客户端
 * 用于调用mall-search服务的接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@FeignClient(name = "mall-search", fallbackFactory = SearchFeignFallback.class)
public interface SearchFeignClient {

    /**
     * 触发全量同步商品数据到ES
     * 
     * @return 同步结果
     */
    @PostMapping("/sync/all")
    Result<Map<String, Object>> syncAll();

    /**
     * 触发增量同步商品数据到ES
     * 
     * @return 同步结果
     */
    @PostMapping("/sync/incremental")
    Result<Map<String, Object>> syncIncremental();
}
