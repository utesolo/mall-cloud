package xyh.dp.mall.job.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;

import java.util.Collections;
import java.util.List;

/**
 * 交易服务Feign降级处理
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class TradeFeignFallback implements FallbackFactory<TradeFeignClient> {

    @Override
    public TradeFeignClient create(Throwable cause) {
        return new TradeFeignClient() {
            @Override
            public Result<List<Object>> getWeeklyHotProducts(Integer topN) {
                log.error("调用交易服务查询热销商品失败，降级处理", cause);
                return Result.fail("交易服务不可用");
            }
        };
    }
}
