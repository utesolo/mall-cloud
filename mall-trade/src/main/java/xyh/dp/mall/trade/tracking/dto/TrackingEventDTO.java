package xyh.dp.mall.trade.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 埋点事件DTO
 * 用于接收前端上报的埋点数据
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "埋点事件请求")
public class TrackingEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     * PRODUCT_VIEW - 查看商品
     * PRODUCT_CLICK - 点击商品
     * MATCH_VIEW - 查看匹配结果
     * MATCH_CLICK - 点击匹配商品
     * MATCH_CONFIRM - 确认匹配
     * ORDER_CREATE - 创建订单
     */
    @Schema(description = "事件类型", required = true, example = "MATCH_CLICK")
    private String eventType;

    /**
     * 种植计划ID
     */
    @Schema(description = "种植计划ID", example = "PLAN202310010001")
    private String planId;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    /**
     * 供销商ID
     */
    @Schema(description = "供销商ID", example = "SUPPLY001")
    private String supplierId;

    /**
     * 用户设备类型: PC, MOBILE, TABLET
     */
    @Schema(description = "设备类型", example = "MOBILE")
    private String deviceType;

    /**
     * 用户来源渠道
     */
    @Schema(description = "来源渠道", example = "wechat_mini")
    private String channel;

    /**
     * 页面停留时长(秒)
     */
    @Schema(description = "页面停留时长(秒)", example = "30")
    private Integer stayDuration;

    /**
     * 扩展数据（JSON格式）
     */
    @Schema(description = "扩展数据(JSON)", example = "{\"page\":\"match_result\"}")
    private String extData;
}
