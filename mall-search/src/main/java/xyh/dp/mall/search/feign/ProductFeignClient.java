package xyh.dp.mall.search.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.search.vo.ProductSearchVO;

import java.util.List;

/**
 * 商品服务Feign客户端
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@FeignClient(name = "mall-product", fallback = ProductFeignFallback.class)
public interface ProductFeignClient {
    
    /**
     * 查询商品详情
     * 
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/product/{id}")
    Result<ProductSearchVO> getById(@PathVariable("id") Long id);
    
    /**
     * 分页查询商品列表（用于全量同步）
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 商品列表
     */
    @GetMapping("/product/page")
    Result<List<ProductSearchVO>> pageQuery(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize
    );
}
