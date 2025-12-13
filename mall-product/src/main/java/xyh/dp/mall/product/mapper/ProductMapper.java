package xyh.dp.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import xyh.dp.mall.product.entity.Product;

/**
 * 商品Mapper
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存（乐观锁方式防止超卖）
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param currentStock 当前库存（用于乐观锁校验）
     * @return 影响行数，0表示失败
     */
    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock = #{currentStock} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, 
                    @Param("quantity") Integer quantity, 
                    @Param("currentStock") Integer currentStock);
}
