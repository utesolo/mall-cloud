mall-cloud/
├── mall-common/                  # 全局公共模块 (工具类、常量、通用返回对象)
├── mall-gateway/                 # Spring Cloud Gateway (API网关)
├── mall-auth/                    # 认证授权服务 (微信登录、JWT签发)
├── mall-product/                 # 商品服务 (管理商品、分类、品牌)
├── mall-file/                    # 文件服务 (上传图片、视频到OSS/MinIO)
├── mall-trade/                   # 交易服务 (购物车、订单、支付回调)
├── mall-search/                  # 搜索服务 (Elasticsearch, 商品搜索)
├── mall-job/                     # 定时任务服务 (处理过期订单、数据统计)
│
├── mall-eureka/                  # 注册中心 (Eureka Server)
├── mall-config/                  # 配置中心 (Spring Cloud Config / Nacos Config)
└── mall-zipkin/                  # 链路追踪 (Sleuth + Zipkin)