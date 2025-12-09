package xyh.dp.mall.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyh.dp.mall.auth.entity.User;

/**
 * 用户Mapper
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
