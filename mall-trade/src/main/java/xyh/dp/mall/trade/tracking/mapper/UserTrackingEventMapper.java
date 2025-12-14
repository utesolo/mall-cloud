package xyh.dp.mall.trade.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyh.dp.mall.trade.tracking.entity.UserTrackingEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为埋点Mapper
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Mapper
public interface UserTrackingEventMapper extends BaseMapper<UserTrackingEvent> {

    /**
     * 查询指定时间范围内的正样本事件
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 正样本事件列表
     */
    @Select("SELECT * FROM user_tracking_event WHERE is_positive = 1 " +
            "AND event_time >= #{startTime} AND event_time <= #{endTime} " +
            "ORDER BY event_time DESC")
    List<UserTrackingEvent> findPositiveEvents(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内的所有事件（用于导出训练数据）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 事件列表
     */
    @Select("SELECT * FROM user_tracking_event " +
            "WHERE event_time >= #{startTime} AND event_time <= #{endTime} " +
            "AND plan_id IS NOT NULL AND product_id IS NOT NULL " +
            "ORDER BY event_time DESC")
    List<UserTrackingEvent> findTrainingEvents(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户最近的行为事件
     * 
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 事件列表
     */
    @Select("SELECT * FROM user_tracking_event WHERE user_id = #{userId} " +
            "ORDER BY event_time DESC LIMIT #{limit}")
    List<UserTrackingEvent> findRecentEventsByUser(@Param("userId") String userId,
                                                    @Param("limit") int limit);

    /**
     * 统计某商品的确认次数
     * 
     * @param productId 商品ID
     * @return 确认次数
     */
    @Select("SELECT COUNT(*) FROM user_tracking_event " +
            "WHERE product_id = #{productId} AND is_positive = 1")
    int countPositiveByProduct(@Param("productId") Long productId);

    /**
     * 统计某种植计划的所有行为事件数
     * 
     * @param planId 种植计划ID
     * @return 事件数
     */
    @Select("SELECT COUNT(*) FROM user_tracking_event WHERE plan_id = #{planId}")
    int countEventsByPlan(@Param("planId") String planId);
}
