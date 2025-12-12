-- 交易服务数据库
CREATE DATABASE IF NOT EXISTS mall_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mall_trade;

-- 订单表
CREATE TABLE `tb_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品主图',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(11) NOT NULL COMMENT '收货人电话',
  `receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态: PENDING-待支付, PAID-已支付, SHIPPED-已发货, RECEIVED-已收货, FINISHED-已完成, CANCELLED-已取消',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- Mock数据
INSERT INTO `tb_order` (`id`, `order_no`, `user_id`, `product_id`, `product_name`, `product_image`, `price`, `quantity`, `total_amount`, `receiver_name`, `receiver_phone`, `receiver_address`, `status`, `create_time`, `pay_time`) VALUES
(1, 'ORD202412080001', 1, 1, '优质番茄种子', 'https://cdn.example.com/product/tomato.jpg', 25.00, 10, 250.00, '张三', '13800138001', '北京市朝阳区XX街道XX号', 'PAID', '2024-12-01 10:30:00', '2024-12-01 10:35:00'),
(2, 'ORD202412080002', 1, 3, '草莓种子', 'https://cdn.example.com/product/strawberry.jpg', 35.00, 5, 175.00, '张三', '13800138001', '北京市朝阳区XX街道XX号', 'SHIPPED', '2024-12-02 14:20:00', '2024-12-02 14:25:00'),
(3, 'ORD202412080003', 2, 5, '优质水稻种子', 'https://cdn.example.com/product/rice.jpg', 28.00, 20, 560.00, '李四', '13800138002', '河北省石家庄市XX区XX村', 'PENDING', '2024-12-08 09:15:00', NULL);

-- 种植计划表（供给匹配）
CREATE TABLE `planting_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` VARCHAR(32) NOT NULL COMMENT '种植计划ID（业务唯一标识）',
  `farmer_id` VARCHAR(32) NOT NULL COMMENT '农户ID',
  `planting_area` DECIMAL(10,2) NOT NULL COMMENT '种植面积(亩)',
  `variety` VARCHAR(100) NOT NULL COMMENT '种植品种',
  `expected_yield` INT NOT NULL COMMENT '预计产量(个)',
  `planting_date` DATE NOT NULL COMMENT '种植时间',
  `target_usage` VARCHAR(100) NOT NULL COMMENT '目标用途',
  `plan_summary` VARCHAR(500) DEFAULT NULL COMMENT '种植计划摘要（自动生成）',
  `region` VARCHAR(100) NOT NULL COMMENT '种植区域',
  `match_score` INT DEFAULT NULL COMMENT '供需匹配度(0-100)',
  `supplier_id` VARCHAR(32) DEFAULT NULL COMMENT '供销商ID',
  `match_time` DATETIME DEFAULT NULL COMMENT '匹配时间',
  `climate_match` VARCHAR(500) DEFAULT NULL COMMENT '区域气候匹配描述',
  `match_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '匹配状态: PENDING-待匹配, MATCHED-已匹配, CONFIRMED-已确认, CANCELLED-已取消',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_id` (`plan_id`),
  KEY `idx_farmer_id` (`farmer_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_match_status` (`match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种植计划表（供给匹配）';

-- Mock数据 - 种植计划
INSERT INTO `planting_plan` (`id`, `plan_id`, `farmer_id`, `planting_area`, `variety`, `expected_yield`, `planting_date`, `target_usage`, `plan_summary`, `region`, `match_score`, `supplier_id`, `match_time`, `climate_match`, `match_status`, `create_time`) VALUES
(1, 'PLAN202310010001', 'FARMER001', 30.00, '鹤首葫芦', 10000, '2023-04-10', '工艺品制作', '山东菏泽·鹤首葫芦·30.0亩·预计产量10000个·用于工艺品制作', '山东菏泽', 85, 'SUPPLY001', '2023-10-01 14:30:00', '当前山东地区适合鹤首葫芦种植', 'CONFIRMED', '2023-03-15 10:00:00'),
(2, 'PLAN202310020002', 'FARMER002', 50.00, '优质小麦', 25000, '2023-10-15', '食用加工', '河南郑州·优质小麦·50.0亩·预计产量25000个·用于食用加工', '河南郑州', 92, 'SUPPLY003', '2023-10-02 09:20:00', '当前河南地区适合优质小麦种植', 'MATCHED', '2023-09-20 14:30:00'),
(3, 'PLAN202312080003', 'FARMER001', 15.00, '草莓', 8000, '2024-03-01', '鲜食销售', '北京大兴·草莓·15.0亩·预计产量8000个·用于鲜食销售', '北京大兴', NULL, NULL, NULL, NULL, 'PENDING', '2024-12-08 11:00:00'),
(4, 'PLAN202312100004', 'FARMER003', 100.00, '水稻', 80000, '2024-04-20', '粮食储备', '江苏南京·水稻·100.0亩·预计产量80000个·用于粮食储备', '江苏南京', 78, 'SUPPLY002', '2024-12-10 16:45:00', '当前江苏地区适合水稻种植', 'MATCHED', '2024-12-05 08:30:00');
