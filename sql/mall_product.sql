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

-- 商品表（种子类商品）
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  
  -- 基础信息
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT NOT NULL COMMENT '商品分类ID',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '商品主图',
  `images` TEXT DEFAULT NULL COMMENT '商品图片列表(JSON数组)',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `specification` VARCHAR(100) DEFAULT NULL COMMENT '商品规格(如: 50克/袋、10粒/包)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '商品状态: ON_SALE-上架, OFF_SALE-下架',
  
  -- 种子特有属性
  `origin` VARCHAR(100) DEFAULT NULL COMMENT '种子产地',
  `variety` VARCHAR(100) DEFAULT NULL COMMENT '种子品种',
  `difficulty` VARCHAR(20) DEFAULT NULL COMMENT '种植难度: EASY-简单, MEDIUM-中等, HARD-困难',
  `growth_cycle` INT DEFAULT NULL COMMENT '生长周期(天)',
  `germination_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '发芽率(%)',
  `purity` DECIMAL(5,2) DEFAULT NULL COMMENT '品种纯度(%)',
  `shelf_life` INT DEFAULT NULL COMMENT '保质期(月)',
  `production_date` DATE DEFAULT NULL COMMENT '生产日期',
  
  -- 种植环境要求(IOT/AI推荐用)
  `min_temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '适宜温度下限(℃)',
  `max_temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '适宜温度上限(℃)',
  `min_humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '适宜湿度下限(%)',
  `max_humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '适宜湿度上限(%)',
  `min_ph` DECIMAL(4,2) DEFAULT NULL COMMENT '适宜土壤PH下限',
  `max_ph` DECIMAL(4,2) DEFAULT NULL COMMENT '适宜土壤PH上限',
  `light_requirement` VARCHAR(20) DEFAULT NULL COMMENT '光照要求: FULL_SUN-全日照, PARTIAL_SUN-半日照, SHADE-耐阴',
  `regions` TEXT DEFAULT NULL COMMENT '适配区域(JSON数组)',
  `planting_seasons` VARCHAR(100) DEFAULT NULL COMMENT '种植季节(JSON数组)',
  
  -- 质量溯源
  `trace_code` VARCHAR(64) DEFAULT NULL COMMENT '溯源码/追溯编号',
  `batch_number` VARCHAR(64) DEFAULT NULL COMMENT '生产批次号',
  `inspection_report_url` VARCHAR(255) DEFAULT NULL COMMENT '质检报告URL',
  `blockchain_hash` VARCHAR(128) DEFAULT NULL COMMENT '区块链存证哈希',
  
  -- 时间戳
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_status` (`status`),
  KEY `idx_trace_code` (`trace_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表(种子类商品)';

-- Mock数据 - 分类
INSERT INTO `category` (`id`, `name`, `parent_id`, `icon`, `sort`, `status`) VALUES
(1, '蒬菜种子', 0, 'https://cdn.example.com/icon/vegetable.png', 1, 'NORMAL'),
(2, '水果种子', 0, 'https://cdn.example.com/icon/fruit.png', 2, 'NORMAL'),
(3, '粮食作物', 0, 'https://cdn.example.com/icon/grain.png', 3, 'NORMAL'),
(4, '花卉种子', 0, 'https://cdn.example.com/icon/flower.png', 4, 'NORMAL');

-- Mock数据 - 商品（包含新字段）
INSERT INTO `product` (
  `id`, `name`, `category_id`, `main_image`, `images`, `description`, `specification`,
  `price`, `stock`, `sales`, `supplier_id`, `status`,
  `origin`, `variety`, `difficulty`, `growth_cycle`, `germination_rate`, `purity`, `shelf_life`, `production_date`,
  `min_temperature`, `max_temperature`, `min_humidity`, `max_humidity`, `min_ph`, `max_ph`, `light_requirement`, `regions`, `planting_seasons`,
  `trace_code`, `batch_number`, `inspection_report_url`
) VALUES
(1, '优质番茄种子', 1, 'https://cdn.example.com/product/tomato.jpg', 
   '["https://cdn.example.com/product/tomato1.jpg","https://cdn.example.com/product/tomato2.jpg"]', 
   '高产优质番茄种子，适合大棚和露地种植', '50克/袋',
   25.00, 1000, 156, 3, 'ON_SALE',
   '山东寿光', '红宝石', 'EASY', 90, 95.50, 99.00, 12, '2025-01-15',
   15.00, 30.00, 60.00, 80.00, 6.00, 7.50, 'FULL_SUN', '["\u534e\u5317","\u534e\u4e1c","\u534e\u4e2d"]', '["\u6625\u5b63","\u79cb\u5b63"]',
   'TRACE202501001', 'BATCH20250115001', 'https://cdn.example.com/report/tomato_report.pdf'),

(2, '黄瓜种子-抗病型', 1, 'https://cdn.example.com/product/cucumber.jpg', 
   '["https://cdn.example.com/product/cucumber1.jpg"]', 
   '抗病性强的黄瓜种子，产量高', '100粒/包',
   18.00, 800, 89, 3, 'ON_SALE',
   '山东寿光', '津绿3号', 'MEDIUM', 60, 92.00, 98.50, 12, '2025-02-01',
   18.00, 32.00, 65.00, 85.00, 5.50, 7.00, 'FULL_SUN', '["\u534e\u5317","\u4e1c\u5317"]', '["\u6625\u5b63"]',
   'TRACE202502001', 'BATCH20250201001', 'https://cdn.example.com/report/cucumber_report.pdf'),

(3, '草莓种子', 2, 'https://cdn.example.com/product/strawberry.jpg', 
   '["https://cdn.example.com/product/strawberry1.jpg","https://cdn.example.com/product/strawberry2.jpg"]', 
   '甜度高的优质草莓种子', '200粒/包',
   35.00, 500, 234, 4, 'ON_SALE',
   '丹东', '红颜', 'HARD', 120, 85.00, 97.00, 18, '2024-11-20',
   10.00, 25.00, 70.00, 90.00, 5.50, 6.80, 'PARTIAL_SUN', '["\u534e\u4e1c","\u534e\u5357"]', '["\u79cb\u5b63"]',
   'TRACE202411001', 'BATCH20241120001', 'https://cdn.example.com/report/strawberry_report.pdf'),

(4, '西瓜种子-无籽型', 2, 'https://cdn.example.com/product/watermelon.jpg', 
   '["https://cdn.example.com/product/watermelon1.jpg"]', 
   '无籽西瓜，口感甜美', '20粒/袋',
   45.00, 600, 178, 4, 'ON_SALE',
   '新疆', '黑美人', 'MEDIUM', 100, 88.00, 98.00, 12, '2025-01-10',
   22.00, 35.00, 50.00, 70.00, 6.00, 7.50, 'FULL_SUN', '["\u534e\u5317","\u534e\u4e2d","\u897f\u5317"]', '["\u6625\u5b63"]',
   'TRACE202501002', 'BATCH20250110001', 'https://cdn.example.com/report/watermelon_report.pdf'),

(5, '优质水稻种子', 3, 'https://cdn.example.com/product/rice.jpg', 
   '["https://cdn.example.com/product/rice1.jpg"]', 
   '高产抗倒伏水稻种子', '500克/袋',
   28.00, 2000, 456, 3, 'ON_SALE',
   '湖南', '湘晚籽', 'EASY', 150, 96.00, 99.50, 24, '2024-12-01',
   20.00, 35.00, 70.00, 95.00, 5.50, 7.00, 'FULL_SUN', '["\u534e\u4e2d","\u534e\u5357","\u897f\u5357"]', '["\u6625\u5b63"]',
   'TRACE202412001', 'BATCH20241201001', 'https://cdn.example.com/report/rice_report.pdf'),

(6, '小麦种子-抗旱型', 3, 'https://cdn.example.com/product/wheat.jpg', 
   '["https://cdn.example.com/product/wheat1.jpg"]', 
   '抗旱能力强的小麦种子', '1000克/袋',
   22.00, 1500, 321, 4, 'ON_SALE',
   '河南', '豪麦', 'EASY', 200, 94.00, 99.00, 24, '2024-10-15',
   5.00, 28.00, 40.00, 70.00, 6.50, 8.00, 'FULL_SUN', '["\u534e\u5317","\u897f\u5317"]', '["\u79cb\u5b63"]',
   'TRACE202410001', 'BATCH20241015001', 'https://cdn.example.com/report/wheat_report.pdf');
