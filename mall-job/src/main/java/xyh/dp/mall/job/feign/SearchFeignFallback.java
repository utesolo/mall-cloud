package xyh.dp.mall.job.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索服务Feign降级处理
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class SearchFeignFallback implements FallbackFactory<SearchFeignClient> {

    @Override
    public SearchFeignClient create(Throwable cause) {
        return new SearchFeignClient() {
            @Override
            public Result<Map<String, Object>> syncAll() {
                log.error("调用搜索服务全量同步失败，降级处理", cause);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", cause.getMessage());
                return Result.fail("搜索服务不可用");
            }

            @Override
            public Result<Map<String, Object>> syncIncremental() {
                log.error("调用搜索服务增量同步失败，降级处理", cause);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", cause.getMessage());
                return Result.fail("搜索服务不可用");
            }
        };
    }
}
