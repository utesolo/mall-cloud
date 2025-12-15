mail-cloud/
│
├── .git/                           # Git 版本控制目录
├── .gitattributes                  # Git 属性配置
├── .gitignore                      # Git 忽略文件配置
├── .mvn/                           # Maven Wrapper 配置目录
├── .qoder/                         # Qoder AI 配置目录
│
├── AGENTS.md                       # AI Agent 指令文档
├── LICENSE                         # 开源许可证
├── README.md                       # 项目说明文档
├── catalogue.md                    # 项目目录说明
├── 原型设计.md                      # 原型设计文档
│
├── pom.xml                         # Maven 父项目配置
├── mvnw                            # Maven Wrapper 脚本(Unix)
├── mvnw.cmd                        # Maven Wrapper 脚本(Windows)
│
├── mall-auth/                      # 认证服务模块(微信登录、JWT)
├── mall-common/                    # 公共模块(工具类、异常处理、统一返回)
├── mall-config/                    # 配置中心模块(Nacos配置)
├── mall-file/                      # 文件服务模块
├── mall-gateway/                   # 网关服务模块(Spring Cloud Gateway)
├── mall-job/                       # 定时任务模块
├── mall-product/                   # 商品服务模块(商品、分类管理)
├── mall-search/                    # 搜索服务模块(Elasticsearch)
├── mall-trade/                     # 交易服务模块(订单、种植计划、匹配)
├── mall-zipkin/                    # 链路追踪模块(Zipkin)
│
├── ml/                             # 机器学习模块(Python)
│   ├── inference_server.py         # 推理服务器
│   ├── requirements.txt            # Python 依赖
│   └── train_matching_model.py     # 模型训练脚本
│
├── qoder/                          # Qoder 规则目录
│   └── rules/
│       └── version.md
│
└── sql/                            # SQL 脚本目录
├── mall_auth.sql               # 认证服务数据库
├── mall_job.sql                # 定时任务数据库
├── mall_product.sql            # 商品服务数据库
└── mall_trade.sql              # 交易服务数据库
