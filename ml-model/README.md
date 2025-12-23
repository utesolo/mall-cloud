# 种子商品匹配机器学习模型

## 项目结构

```
ml-model/
├── requirements.txt          # Python依赖列表
├── train_model.py           # 训练脚本
├── predict_api.py           # 预测API服务
├── models/                  # 保存的模型文件
│   ├── match_model.pkl      # 训练好的随机森林模型
│   ├── feature_scaler.pkl   # 特征标准化器
│   ├── feature_importance.csv # 特征重要性排序
│   └── training_metrics.txt  # 训练评估指标
└── data/                    # 训练数据目录
    └── training_data.csv    # 从Java导出的埋点数据
```

## 安装

```bash
# 创建虚拟环境（推荐）
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt
```

## 数据准备

从Java应用导出训练数据：

```bash
# 调用Java API导出CSV
curl -X GET "http://localhost:8084/tracking/export/training-data?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59" \
  -o training_data.csv
```

CSV格式：
```
plan_id,product_id,variety_score,region_score,climate_score,season_score,quality_score,intent_score,total_score,match_grade,is_positive
PLAN202410010001,1001,90.00,85.00,88.00,90.00,88.00,75.00,85.50,A,1
PLAN202410010002,1002,75.00,70.00,72.00,75.00,78.00,65.00,72.50,B,1
...
```

## 模型训练

```bash
# 基础用法
python train_model.py data/training_data.csv

# 指定模型保存目录
python train_model.py data/training_data.csv ./models

# 输出示例
# Loading training data from data/training_data.csv
# Loaded 10000 records from data/training_data.csv
# Starting model training...
# Model training completed
# Model Evaluation Metrics:
# ================================================== =========
# Training   - Accuracy: 0.8523, AUC: 0.9145
# Testing    - Accuracy: 0.8412, AUC: 0.8987
# Cross-Val  - Mean: 0.8450, Std: 0.0087
# ===========================================================
```

## 模型特性

- **算法**: RandomForestClassifier (随机森林)
- **树数量**: 200
- **最大深度**: 15
- **特征数**: 6维特征向量
- **输出**: 用户确认概率 (0-1) → 转换为分数 (0-100)

## 启动预测API服务

```bash
# 基础用法（端口5000）
python predict_api.py

# 指定模型目录和端口
python predict_api.py ./models 5000

# 输出示例
# Starting API server on port 5000
# Model directory: ./models
# Running on http://0.0.0.0:5000
```

## API接口

### 1. 健康检查

```bash
curl http://localhost:5000/api/health

# 响应
{
  "status": "healthy",
  "model_loaded": true,
  "scaler_loaded": true
}
```

### 2. 模型信息

```bash
curl http://localhost:5000/api/model-info

# 响应
{
  "model_type": "RandomForestClassifier",
  "n_estimators": 200,
  "max_depth": 15,
  "features": [
    "variety_score",
    "region_score",
    "climate_score",
    "season_score",
    "quality_score",
    "intent_score"
  ],
  "feature_count": 6
}
```

### 3. 单条预测

```bash
curl -X POST http://localhost:5000/api/predict \
  -H "Content-Type: application/json" \
  -d '{
    "variety_score": 90.0,
    "region_score": 85.0,
    "climate_score": 88.0,
    "season_score": 90.0,
    "quality_score": 88.0,
    "intent_score": 75.0
  }'

# 响应
{
  "score": 85.50,
  "confidence": 0.8550,
  "grade": "A",
  "features": {
    "variety_score": {
      "value": 90.0,
      "importance": 0.25
    },
    "region_score": {
      "value": 85.0,
      "importance": 0.20
    },
    ...
  }
}
```

### 4. 批量预测

```bash
curl -X POST http://localhost:5000/api/predict \
  -H "Content-Type: application/json" \
  -d '[
    {
      "variety_score": 90.0,
      "region_score": 85.0,
      "climate_score": 88.0,
      "season_score": 90.0,
      "quality_score": 88.0,
      "intent_score": 75.0
    },
    {
      "variety_score": 75.0,
      "region_score": 70.0,
      "climate_score": 72.0,
      "season_score": 75.0,
      "quality_score": 78.0,
      "intent_score": 65.0
    }
  ]'

# 响应
{
  "count": 2,
  "results": [
    {
      "index": 0,
      "score": 85.50,
      "confidence": 0.8550,
      "grade": "A"
    },
    {
      "index": 1,
      "score": 72.50,
      "confidence": 0.7250,
      "grade": "B"
    }
  ]
}
```

## 在Java中集成

在`MatchScoreCalculator`中调用Python API替换规则引擎：

```java
@Service
public class MLMatchService {
    
    private final RestTemplate restTemplate;
    
    public MatchFeature calculateMLScore(PlantingPlan plan, ProductDTO product) {
        // 构建特征
        Map<String, BigDecimal> features = new HashMap<>();
        features.put("variety_score", varietyScore);
        features.put("region_score", regionScore);
        features.put("climate_score", climateScore);
        features.put("season_score", seasonScore);
        features.put("quality_score", qualityScore);
        features.put("intent_score", intentScore);
        
        // 调用Python API
        String response = restTemplate.postForObject(
            "http://localhost:5000/api/predict",
            features,
            String.class
        );
        
        // 解析响应获取分数
        JSONObject json = new JSONObject(response);
        BigDecimal score = new BigDecimal(json.getDouble("score"));
        String grade = json.getString("grade");
        
        return feature.withTotalScore(score).withMatchGrade(grade);
    }
}
```

## 模型评估指标

| 指标 | 说明 | 目标值 |
|-----|------|--------|
| Accuracy | 预测准确率 | > 80% |
| AUC-ROC | 曲线下面积 | > 0.85 |
| Precision | 正样本准确率 | > 80% |
| Recall | 正样本召回率 | > 75% |
| F1-Score | 精准度和召回率的调和平均 | > 0.75 |

## 特征重要性

模型会输出各特征对预测结果的重要性排序，帮助理解哪些因素对匹配最关键。

例如：
```
feature,importance
variety_score,0.2500
region_score,0.2000
climate_score,0.1800
season_score,0.1700
quality_score,0.1500
intent_score,0.0700
```

## 常见问题

### Q: 如何更新模型？
A: 定期从Java应用导出新的埋点数据，重新运行训练脚本即可。

### Q: 模型需要多少训练数据？
A: 建议至少1000条样本，50%以上的正样本。数据越多，模型效果越好。

### Q: 如何处理类别不平衡？
A: 脚本已配置`class_weight='balanced'`，会自动调整权重处理不平衡问题。

### Q: 能否在线更新模型？
A: 不建议。应离线训练好新模型后替换，避免生产环境中断。

## 性能优化

- 批量预测时使用批处理API，比单条预测快
- 模型已进行特征标准化，不需要额外预处理
- Python服务可使用Gunicorn+Nginx部署到生产环境

## 联系方式

如有问题，请查看训练日志或模型评估文件。
