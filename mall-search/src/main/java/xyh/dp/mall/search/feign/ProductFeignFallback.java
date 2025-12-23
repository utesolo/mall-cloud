package xyh.dp.mall.search.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.search.vo.ProductSearchVO;

import java.util.List;

/**
 * 商品服务Feign降级处理
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProductFeignFallback implements ProductFeignClient {
    
    @Override
    public Result<ProductSearchVO> getById(Long id) {
        log.error("调用商品服务失败，降级处理: getById({})", id);
        return Result.error("商品服务暂时不可用");
    }
    
    @Override
    public Result<List<ProductSearchVO>> pageQuery(Integer pageNum, Integer pageSize) {
        log.error("调用商品服务失败，降级处理: pageQuery({}, {})", pageNum, pageSize);
        return Result.error("商品服务暂时不可用");
    }
}
