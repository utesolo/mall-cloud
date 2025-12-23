package xyh.dp.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyh.dp.mall.trade.entity.CartItem;

/**
 * 购物车Mapper接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
