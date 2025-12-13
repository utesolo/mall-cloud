package xyh.dp.mall.trade.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.feign.fallback.ProductFeignFallback;

/**
 * 商品服务Feign客户端
 * 用于跨服务调用商品服务接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@FeignClient(name = "mall-product", fallbackFactory = ProductFeignFallback.class)
public interface ProductFeignClient {

    /**
     * 根据商品ID查询商品信息
     * 
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/product/{id}")
    Result<ProductDTO> getProductById(@PathVariable("id") Long id);

    /**
     * 扣减商品库存
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 操作结果
     */
    @PostMapping("/product/stock/deduct")
    Result<Boolean> deductStock(@RequestParam("productId") Long productId, 
                                 @RequestParam("quantity") Integer quantity);

    /**
     * 恢复商品库存（用于订单取消/失败时回滚）
     * 
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 操作结果
     */
    @PostMapping("/product/stock/restore")
    Result<Boolean> restoreStock(@RequestParam("productId") Long productId, 
                                  @RequestParam("quantity") Integer quantity);

    /**
     * 增加商品销量
     * 
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 操作结果
     */
    @PostMapping("/product/sales/increase")
    Result<Boolean> increaseSales(@RequestParam("productId") Long productId, 
                                   @RequestParam("quantity") Integer quantity);
}
