package xyh.dp.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@TableName("product")
public class Product implements Serializable {
    // TODO 对于种子类商品添加适宜温度、湿度、盐碱度等字段

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品图片列表(JSON数组)
     */
    private String images;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 适配区域(JSON数组)
     */
    private String regions;

    /**
     * 种植难度: EASY-简单, MEDIUM-中等, HARD-困难
     */
    private String difficulty;

    /**
     * 生长周期(天)
     */
    private Integer growthCycle;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 商品状态: ON_SALE-上架, OFF_SALE-下架
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
