"""
机器学习模型训练脚本
使用scikit-learn基于用户埋点数据训练种子商品匹配模型

特征列:
  - variety_score: 品种一致性得分 (0-100)
  - region_score: 区域适配得分 (0-100)
  - climate_score: 气候匹配得分 (0-100)
  - season_score: 季节匹配得分 (0-100)
  - quality_score: 种子质量得分 (0-100)
  - intent_score: 供需意图得分 (0-100)

目标变量:
  - is_positive: 用户是否确认/购买 (0/1)

使用随机森林分类器预测用户确认概率
"""

import pandas as pd
import numpy as np
import joblib
import logging
import sys
from pathlib import Path
from datetime import datetime

from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score, accuracy_score

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class MatchModelTrainer:
    """
    种子商品匹配模型训练器
    """
    
    # 特征列名
    FEATURE_COLUMNS = [
        'variety_score',
        'region_score', 
        'climate_score',
        'season_score',
        'quality_score',
        'intent_score'
    ]
    
    # 目标变量列名
    TARGET_COLUMN = 'is_positive'
    
    def __init__(self, model_dir='./models', test_size=0.2, random_state=42):
        """
        初始化训练器
        
        Args:
            model_dir: 模型保存目录
            test_size: 测试集比例
            random_state: 随机种子
        """
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(parents=True, exist_ok=True)
        
        self.test_size = test_size
        self.random_state = random_state
        
        self.model = None
        self.scaler = None
        self.feature_importance = None
        self.training_metrics = {}
        
        logger.info(f"MatchModelTrainer initialized with model_dir={model_dir}")
    
    def load_training_data(self, csv_file):
        """
        加载CSV格式的训练数据
        
        Args:
            csv_file: CSV文件路径
            
        Returns:
            加载成功返回True，否则False
        """
        try:
            logger.info(f"Loading training data from {csv_file}")
            
            df = pd.read_csv(csv_file)
            logger.info(f"Loaded {len(df)} records from {csv_file}")
            
            # 验证必需列是否存在
            missing_columns = [col for col in self.FEATURE_COLUMNS + [self.TARGET_COLUMN]
                             if col not in df.columns]
            if missing_columns:
                logger.error(f"Missing required columns: {missing_columns}")
                return None
            
            # 数据清洗：移除包含缺失值的行
            initial_count = len(df)
            df = df.dropna(subset=self.FEATURE_COLUMNS + [self.TARGET_COLUMN])
            dropped = initial_count - len(df)
            if dropped > 0:
                logger.warning(f"Dropped {dropped} rows with missing values")
            
            # 数据验证：确保特征值在0-100范围内
            for col in self.FEATURE_COLUMNS:
                df = df[(df[col] >= 0) & (df[col] <= 100)]
            
            # 验证目标变量值
            df = df[df[self.TARGET_COLUMN].isin([0, 1])]
            
            logger.info(f"After cleaning: {len(df)} records available")
            
            if len(df) == 0:
                logger.error("No valid training data after cleaning")
                return None
            
            return df
            
        except Exception as e:
            logger.error(f"Error loading training data: {e}", exc_info=True)
            return None
    
    def train(self, df):
        """
        训练匹配模型
        
        Args:
            df: 训练数据DataFrame
            
        Returns:
            训练成功返回True，否则False
        """
        try:
            logger.info("Starting model training...")
            
            # 检查类别分布
            class_dist = df[self.TARGET_COLUMN].value_counts()
            logger.info(f"Class distribution - Positive: {class_dist.get(1, 0)}, Negative: {class_dist.get(0, 0)}")
            
            # 提取特征和目标变量
            X = df[self.FEATURE_COLUMNS].astype(np.float32)
            y = df[self.TARGET_COLUMN].astype(np.int32)
            
            # 分割训练集和测试集
            X_train, X_test, y_train, y_test = train_test_split(
                X, y,
                test_size=self.test_size,
                random_state=self.random_state,
                stratify=y  # 确保类别分布一致
            )
            
            logger.info(f"Training set size: {len(X_train)}, Test set size: {len(X_test)}")
            
            # 特征标准化
            self.scaler = StandardScaler()
            X_train_scaled = self.scaler.fit_transform(X_train)
            X_test_scaled = self.scaler.transform(X_test)
            
            # 训练随机森林分类器
            logger.info("Training RandomForestClassifier...")
            self.model = RandomForestClassifier(
                n_estimators=200,           # 树的数量
                max_depth=15,               # 树的最大深度
                min_samples_split=5,        # 分割节点的最小样本数
                min_samples_leaf=2,         # 叶子节点的最小样本数
                max_features='sqrt',        # 每次分割的最大特征数
                random_state=self.random_state,
                n_jobs=-1,                  # 使用所有CPU核心
                class_weight='balanced'     # 处理类别不平衡
            )
            
            self.model.fit(X_train_scaled, y_train)
            logger.info("Model training completed")
            
            # 评估模型
            self._evaluate_model(X_train_scaled, X_test_scaled, y_train, y_test)
            
            # 保存特征重要性
            self.feature_importance = pd.DataFrame({
                'feature': self.FEATURE_COLUMNS,
                'importance': self.model.feature_importances_
            }).sort_values('importance', ascending=False)
            
            logger.info("Feature importance:")
            logger.info(self.feature_importance.to_string())
            
            return True
            
        except Exception as e:
            logger.error(f"Error during model training: {e}", exc_info=True)
            return False
    
    def _evaluate_model(self, X_train_scaled, X_test_scaled, y_train, y_test):
        """
        评估模型性能
        
        Args:
            X_train_scaled: 缩放后的训练特征
            X_test_scaled: 缩放后的测试特征
            y_train: 训练目标变量
            y_test: 测试目标变量
        """
        try:
            # 训练集性能
            train_pred = self.model.predict(X_train_scaled)
            train_pred_proba = self.model.predict_proba(X_train_scaled)[:, 1]
            train_acc = accuracy_score(y_train, train_pred)
            train_auc = roc_auc_score(y_train, train_pred_proba)
            
            # 测试集性能
            test_pred = self.model.predict(X_test_scaled)
            test_pred_proba = self.model.predict_proba(X_test_scaled)[:, 1]
            test_acc = accuracy_score(y_test, test_pred)
            test_auc = roc_auc_score(y_test, test_pred_proba)
            
            # 交叉验证
            cv_scores = cross_val_score(self.model, X_train_scaled, y_train, cv=5)
            
            self.training_metrics = {
                'train_accuracy': float(train_acc),
                'train_auc': float(train_auc),
                'test_accuracy': float(test_acc),
                'test_auc': float(test_auc),
                'cv_mean': float(cv_scores.mean()),
                'cv_std': float(cv_scores.std())
            }
            
            logger.info(f"\n{'='*50}")
            logger.info("Model Evaluation Metrics:")
            logger.info(f"{'='*50}")
            logger.info(f"Training   - Accuracy: {train_acc:.4f}, AUC: {train_auc:.4f}")
            logger.info(f"Testing    - Accuracy: {test_acc:.4f}, AUC: {test_auc:.4f}")
            logger.info(f"Cross-Val  - Mean: {cv_scores.mean():.4f}, Std: {cv_scores.std():.4f}")
            logger.info(f"{'='*50}\n")
            
            # 详细分类报告
            logger.info("Test Set Classification Report:")
            logger.info("\n" + classification_report(y_test, test_pred, 
                                                     target_names=['Negative', 'Positive']))
            
            # 混淆矩阵
            cm = confusion_matrix(y_test, test_pred)
            logger.info(f"Confusion Matrix:\n{cm}")
            
        except Exception as e:
            logger.error(f"Error evaluating model: {e}", exc_info=True)
    
    def save_model(self):
        """
        保存训练好的模型和缩放器
        
        Returns:
            保存成功返回True，否则False
        """
        try:
            if self.model is None or self.scaler is None:
                logger.error("Model or scaler not trained yet")
                return False
            
            # 保存模型
            model_path = self.model_dir / 'match_model.pkl'
            joblib.dump(self.model, model_path)
            logger.info(f"Model saved to {model_path}")
            
            # 保存缩放器
            scaler_path = self.model_dir / 'feature_scaler.pkl'
            joblib.dump(self.scaler, scaler_path)
            logger.info(f"Scaler saved to {scaler_path}")
            
            # 保存特征重要性
            if self.feature_importance is not None:
                importance_path = self.model_dir / 'feature_importance.csv'
                self.feature_importance.to_csv(importance_path, index=False)
                logger.info(f"Feature importance saved to {importance_path}")
            
            # 保存训练指标
            if self.training_metrics:
                metrics_path = self.model_dir / 'training_metrics.txt'
                with open(metrics_path, 'w') as f:
                    f.write(f"Training completed: {datetime.now().isoformat()}\n\n")
                    for key, value in self.training_metrics.items():
                        f.write(f"{key}: {value}\n")
                logger.info(f"Training metrics saved to {metrics_path}")
            
            return True
            
        except Exception as e:
            logger.error(f"Error saving model: {e}", exc_info=True)
            return False
    
    def predict(self, features):
        """
        使用模型进行预测
        
        Args:
            features: 形状为(n_samples, n_features)的特征数组
            
        Returns:
            预测概率(0-100分)
        """
        if self.model is None or self.scaler is None:
            logger.error("Model not loaded")
            return None
        
        try:
            # 确保输入是DataFrame且列顺序正确
            if isinstance(features, dict):
                features = pd.DataFrame([features])
            elif isinstance(features, list):
                features = pd.DataFrame(features, columns=self.FEATURE_COLUMNS)
            
            # 提取特征列
            X = features[self.FEATURE_COLUMNS].astype(np.float32)
            
            # 标准化
            X_scaled = self.scaler.transform(X)
            
            # 预测概率（positive类的概率）
            proba = self.model.predict_proba(X_scaled)[:, 1]
            
            # 转换为0-100分
            scores = (proba * 100).round(2)
            
            return scores
            
        except Exception as e:
            logger.error(f"Error during prediction: {e}", exc_info=True)
            return None


def main():
    """
    主函数：训练模型
    """
    import sys
    
    # 检查命令行参数
    if len(sys.argv) < 2:
        print("Usage: python train_model.py <csv_file> [model_dir]")
        print("Example: python train_model.py training_data.csv ./models")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    model_dir = sys.argv[2] if len(sys.argv) > 2 else './models'
    
    # 初始化训练器
    trainer = MatchModelTrainer(model_dir=model_dir)
    
    # 加载数据
    df = trainer.load_training_data(csv_file)
    if df is None:
        logger.error("Failed to load training data")
        sys.exit(1)
    
    # 训练模型
    if not trainer.train(df):
        logger.error("Failed to train model")
        sys.exit(1)
    
    # 保存模型
    if not trainer.save_model():
        logger.error("Failed to save model")
        sys.exit(1)
    
    logger.info("Model training completed successfully!")
    sys.exit(0)


if __name__ == '__main__':
    main()
