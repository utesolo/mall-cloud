package xyh.dp.mall.trade.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;

/**
 * 商品服务Feign降级处理
 * 当商品服务不可用时提供降级响应
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProductFeignFallback implements FallbackFactory<ProductFeignClient> {

    /**
     * 创建降级实例
     * 
     * @param cause 触发降级的异常
     * @return 降级处理实现
     */
    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("商品服务调用失败，触发降级: {}", cause.getMessage(), cause);
        
        return new ProductFeignClient() {
            
            /**
             * 查询商品降级处理
             * 
             * @param id 商品ID
             * @return 降级结果
             */
            @Override
            public Result<ProductDTO> getProductById(Long id) {
                log.warn("商品服务降级: getProductById({})", id);
                return Result.error(503, "商品服务暂时不可用，请稍后重试");
            }

            /**
             * 扣减库存降级处理
             * 
             * @param productId 商品ID
             * @param quantity 扣减数量
             * @return 降级结果
             */
            @Override
            public Result<Boolean> deductStock(Long productId, Integer quantity) {
                log.warn("商品服务降级: deductStock({}, {})", productId, quantity);
                return Result.error(503, "库存服务暂时不可用，请稍后重试");
            }

            /**
             * 恢复库存降级处理
             * 
             * @param productId 商品ID
             * @param quantity 恢复数量
             * @return 降级结果
             */
            @Override
            public Result<Boolean> restoreStock(Long productId, Integer quantity) {
                log.warn("商品服务降级: restoreStock({}, {})", productId, quantity);
                return Result.error(503, "库存服务暂时不可用，请稍后重试");
            }

            /**
             * 增加销量降级处理
             * 
             * @param productId 商品ID
             * @param quantity 增加数量
             * @return 降级结果
             */
            @Override
            public Result<Boolean> increaseSales(Long productId, Integer quantity) {
                log.warn("商品服务降级: increaseSales({}, {})", productId, quantity);
                return Result.error(503, "销量服务暂时不可用，请稍后重试");
            }
        };
    }
}
