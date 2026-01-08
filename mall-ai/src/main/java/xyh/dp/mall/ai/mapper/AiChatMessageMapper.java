package xyh.dp.mall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyh.dp.mall.ai.entity.AiChatMessage;

/**
 * 对话消息Mapper接口
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
