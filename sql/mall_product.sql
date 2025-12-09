-- 商品服务数据库
CREATE DATABASE IF NOT EXISTS mall_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mall_product;

-- 商品分类表
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL-正常, DISABLED-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT NOT NULL COMMENT '商品分类ID',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '商品主图',
  `images` TEXT DEFAULT NULL COMMENT '商品图片列表(JSON数组)',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `regions` TEXT DEFAULT NULL COMMENT '适配区域(JSON数组)',
  `difficulty` VARCHAR(20) DEFAULT NULL COMMENT '种植难度: EASY-简单, MEDIUM-中等, HARD-困难',
  `growth_cycle` INT DEFAULT NULL COMMENT '生长周期(天)',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '商品状态: ON_SALE-上架, OFF_SALE-下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- Mock数据 - 分类
INSERT INTO `category` (`id`, `name`, `parent_id`, `icon`, `sort`, `status`) VALUES
(1, '蔬菜种子', 0, 'https://cdn.example.com/icon/vegetable.png', 1, 'NORMAL'),
(2, '水果种子', 0, 'https://cdn.example.com/icon/fruit.png', 2, 'NORMAL'),
(3, '粮食作物', 0, 'https://cdn.example.com/icon/grain.png', 3, 'NORMAL'),
(4, '花卉种子', 0, 'https://cdn.example.com/icon/flower.png', 4, 'NORMAL');

-- Mock数据 - 商品
INSERT INTO `product` (`id`, `name`, `category_id`, `main_image`, `images`, `description`, `price`, `stock`, `sales`, `regions`, `difficulty`, `growth_cycle`, `supplier_id`, `status`) VALUES
(1, '优质番茄种子', 1, 'https://cdn.example.com/product/tomato.jpg', '["https://cdn.example.com/product/tomato1.jpg","https://cdn.example.com/product/tomato2.jpg"]', '高产优质番茄种子，适合大棚和露地种植', 25.00, 1000, 156, '["华北","华东","华中"]', 'EASY', 90, 3, 'ON_SALE'),
(2, '黄瓜种子-抗病型', 1, 'https://cdn.example.com/product/cucumber.jpg', '["https://cdn.example.com/product/cucumber1.jpg"]', '抗病性强的黄瓜种子，产量高', 18.00, 800, 89, '["华北","东北"]', 'MEDIUM', 60, 3, 'ON_SALE'),
(3, '草莓种子', 2, 'https://cdn.example.com/product/strawberry.jpg', '["https://cdn.example.com/product/strawberry1.jpg","https://cdn.example.com/product/strawberry2.jpg"]', '甜度高的优质草莓种子', 35.00, 500, 234, '["华东","华南"]', 'HARD', 120, 4, 'ON_SALE'),
(4, '西瓜种子-无籽型', 2, 'https://cdn.example.com/product/watermelon.jpg', '["https://cdn.example.com/product/watermelon1.jpg"]', '无籽西瓜，口感甜美', 45.00, 600, 178, '["华北","华中","西北"]', 'MEDIUM', 100, 4, 'ON_SALE'),
(5, '优质水稻种子', 3, 'https://cdn.example.com/product/rice.jpg', '["https://cdn.example.com/product/rice1.jpg"]', '高产抗倒伏水稻种子', 28.00, 2000, 456, '["华中","华南","西南"]', 'EASY', 150, 3, 'ON_SALE'),
(6, '小麦种子-抗旱型', 3, 'https://cdn.example.com/product/wheat.jpg', '["https://cdn.example.com/product/wheat1.jpg"]', '抗旱能力强的小麦种子', 22.00, 1500, 321, '["华北","西北"]', 'EASY', 200, 4, 'ON_SALE');
