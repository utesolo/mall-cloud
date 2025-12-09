-- 认证授权数据库
CREATE DATABASE IF NOT EXISTS mall_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mall_auth;

-- 用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` VARCHAR(128) NOT NULL COMMENT '微信OpenID',
  `unionid` VARCHAR(128) DEFAULT NULL COMMENT '微信UnionID',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
  `user_type` VARCHAR(20) DEFAULT NULL COMMENT '用户类型: FARMER-农户, SUPPLIER-供销商',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `business_license` VARCHAR(100) DEFAULT NULL COMMENT '营业执照号(供销商)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '账户状态: NORMAL-正常, DISABLED-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Mock数据
INSERT INTO `user` (`id`, `openid`, `nickname`, `avatar`, `user_type`, `phone`, `real_name`, `status`) VALUES
(1, 'mock_openid_farmer_001', '农户张三', 'https://thirdwx.qlogo.cn/mmopen/default_avatar.png', 'FARMER', '13800138001', '张三', 'NORMAL'),
(2, 'mock_openid_farmer_002', '农户李四', 'https://thirdwx.qlogo.cn/mmopen/default_avatar.png', 'FARMER', '13800138002', '李四', 'NORMAL'),
(3, 'mock_openid_supplier_001', '供销商王五', 'https://thirdwx.qlogo.cn/mmopen/default_avatar.png', 'SUPPLIER', '13800138003', '王五', 'NORMAL'),
(4, 'mock_openid_supplier_002', '供销商赵六', 'https://thirdwx.qlogo.cn/mmopen/default_avatar.png', 'SUPPLIER', '13800138004', '赵六', 'NORMAL');
