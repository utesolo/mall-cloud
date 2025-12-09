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
