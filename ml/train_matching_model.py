#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
种植计划与商品匹配模型训练脚本

基于用户行为埋点数据训练匹配预测模型
特征维度：品种一致性、区域适配、气候匹配、季节匹配、种子质量、供需意图

作者: mall-cloud
版本: 1.0.0
"""

import os
import sys
import argparse
import logging
from datetime import datetime
from pathlib import Path

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    roc_auc_score, confusion_matrix, classification_report
)
import joblib

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('training.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)


class MatchingModelTrainer:
    """匹配模型训练器"""
    
    # 特征列名
    FEATURE_COLUMNS = [
        'variety_score',    # 品种一致性
        'region_score',     # 区域适配
        'climate_score',    # 气候匹配
        'season_score',     # 季节匹配
        'quality_score',    # 种子质量
        'intent_score'      # 供需意图
    ]
    
    # 标签列名
    LABEL_COLUMN = 'is_positive'
    
    def __init__(self, output_dir: str = './models'):
        """
        初始化训练器
        
        Args:
            output_dir: 模型输出目录
        """
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.scaler = StandardScaler()
        self.best_model = None
        self.best_model_name = None
        
    def load_data(self, csv_path: str) -> pd.DataFrame:
        """
        加载CSV数据
        
        Args:
            csv_path: CSV文件路径
            
        Returns:
            DataFrame数据
        """
        logger.info(f"加载数据: {csv_path}")
        df = pd.read_csv(csv_path)
        logger.info(f"数据形状: {df.shape}")
        logger.info(f"列名: {list(df.columns)}")
        return df
    
    def preprocess_data(self, df: pd.DataFrame) -> tuple:
        """
        数据预处理
        
        Args:
            df: 原始数据
            
        Returns:
            (X, y) 特征矩阵和标签向量
        """
        logger.info("开始数据预处理...")
        
        # 检查必需列
        missing_cols = [col for col in self.FEATURE_COLUMNS if col not in df.columns]
        if missing_cols:
            raise ValueError(f"缺少必需列: {missing_cols}")
        
        if self.LABEL_COLUMN not in df.columns:
            raise ValueError(f"缺少标签列: {self.LABEL_COLUMN}")
        
        # 提取特征和标签
        X = df[self.FEATURE_COLUMNS].copy()
        y = df[self.LABEL_COLUMN].copy()
        
        # 处理缺失值
        X = X.fillna(0)
        
        # 处理异常值（限制在0-100范围内）
        for col in self.FEATURE_COLUMNS:
            X[col] = X[col].clip(0, 100)
        
        logger.info(f"特征矩阵形状: {X.shape}")
        logger.info(f"正样本数量: {y.sum()}, 负样本数量: {len(y) - y.sum()}")
        logger.info(f"正样本比例: {y.mean():.2%}")
        
        return X, y
    
    def train_and_evaluate(self, X: pd.DataFrame, y: pd.Series, 
                           test_size: float = 0.2) -> dict:
        """
        训练并评估多个模型
        
        Args:
            X: 特征矩阵
            y: 标签向量
            test_size: 测试集比例
            
        Returns:
            评估结果字典
        """
        logger.info("开始训练模型...")
        
        # 划分训练集和测试集
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=42, stratify=y
        )
        
        # 特征标准化
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # 定义模型
        models = {
            'LogisticRegression': LogisticRegression(
                random_state=42, max_iter=1000, class_weight='balanced'
            ),
            'RandomForest': RandomForestClassifier(
                n_estimators=100, random_state=42, class_weight='balanced'
            ),
            'GradientBoosting': GradientBoostingClassifier(
                n_estimators=100, random_state=42
            ),
            'MLP': MLPClassifier(
                hidden_layer_sizes=(64, 32), random_state=42, max_iter=500
            )
        }
        
        results = {}
        best_f1 = 0
        
        for name, model in models.items():
            logger.info(f"训练模型: {name}")
            
            # 训练
            model.fit(X_train_scaled, y_train)
            
            # 预测
            y_pred = model.predict(X_test_scaled)
            y_prob = model.predict_proba(X_test_scaled)[:, 1] if hasattr(model, 'predict_proba') else None
            
            # 评估
            metrics = self._evaluate_model(y_test, y_pred, y_prob)
            results[name] = metrics
            
            logger.info(f"{name} - Accuracy: {metrics['accuracy']:.4f}, "
                       f"Precision: {metrics['precision']:.4f}, "
                       f"Recall: {metrics['recall']:.4f}, "
                       f"F1: {metrics['f1']:.4f}")
            
            # 更新最佳模型
            if metrics['f1'] > best_f1:
                best_f1 = metrics['f1']
                self.best_model = model
                self.best_model_name = name
        
        logger.info(f"最佳模型: {self.best_model_name} (F1: {best_f1:.4f})")
        
        return results
    
    def _evaluate_model(self, y_true, y_pred, y_prob=None) -> dict:
        """
        评估模型性能
        
        Args:
            y_true: 真实标签
            y_pred: 预测标签
            y_prob: 预测概率
            
        Returns:
            评估指标字典
        """
        metrics = {
            'accuracy': accuracy_score(y_true, y_pred),
            'precision': precision_score(y_true, y_pred, zero_division=0),
            'recall': recall_score(y_true, y_pred, zero_division=0),
            'f1': f1_score(y_true, y_pred, zero_division=0),
            'confusion_matrix': confusion_matrix(y_true, y_pred).tolist()
        }
        
        if y_prob is not None:
            try:
                metrics['auc'] = roc_auc_score(y_true, y_prob)
            except ValueError:
                metrics['auc'] = 0.0
        
        return metrics
    
    def hyperparameter_tuning(self, X: pd.DataFrame, y: pd.Series) -> dict:
        """
        超参数调优
        
        Args:
            X: 特征矩阵
            y: 标签向量
            
        Returns:
            最佳参数
        """
        logger.info("开始超参数调优...")
        
        X_scaled = self.scaler.fit_transform(X)
        
        # 随机森林参数网格
        param_grid = {
            'n_estimators': [50, 100, 200],
            'max_depth': [5, 10, 20, None],
            'min_samples_split': [2, 5, 10],
            'min_samples_leaf': [1, 2, 4]
        }
        
        rf = RandomForestClassifier(random_state=42, class_weight='balanced')
        grid_search = GridSearchCV(
            rf, param_grid, cv=5, scoring='f1', n_jobs=-1, verbose=1
        )
        grid_search.fit(X_scaled, y)
        
        logger.info(f"最佳参数: {grid_search.best_params_}")
        logger.info(f"最佳F1得分: {grid_search.best_score_:.4f}")
        
        self.best_model = grid_search.best_estimator_
        self.best_model_name = 'RandomForest_Tuned'
        
        return grid_search.best_params_
    
    def save_model(self, model_name: str = None):
        """
        保存模型
        
        Args:
            model_name: 模型名称（可选）
        """
        if self.best_model is None:
            raise ValueError("没有可保存的模型，请先训练模型")
        
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        name = model_name or self.best_model_name
        
        # 保存模型
        model_path = self.output_dir / f'{name}_{timestamp}.pkl'
        joblib.dump(self.best_model, model_path)
        logger.info(f"模型已保存: {model_path}")
        
        # 保存标准化器
        scaler_path = self.output_dir / f'scaler_{timestamp}.pkl'
        joblib.dump(self.scaler, scaler_path)
        logger.info(f"标准化器已保存: {scaler_path}")
        
        # 保存模型元信息
        meta = {
            'model_name': name,
            'feature_columns': self.FEATURE_COLUMNS,
            'timestamp': timestamp,
            'model_path': str(model_path),
            'scaler_path': str(scaler_path)
        }
        meta_path = self.output_dir / f'meta_{timestamp}.json'
        import json
        with open(meta_path, 'w', encoding='utf-8') as f:
            json.dump(meta, f, indent=2, ensure_ascii=False)
        logger.info(f"元信息已保存: {meta_path}")
        
        return model_path
    
    def load_model(self, model_path: str, scaler_path: str = None):
        """
        加载模型
        
        Args:
            model_path: 模型文件路径
            scaler_path: 标准化器文件路径
        """
        logger.info(f"加载模型: {model_path}")
        self.best_model = joblib.load(model_path)
        
        if scaler_path:
            logger.info(f"加载标准化器: {scaler_path}")
            self.scaler = joblib.load(scaler_path)
    
    def predict(self, features: dict) -> tuple:
        """
        预测匹配概率
        
        Args:
            features: 特征字典
            
        Returns:
            (预测标签, 预测概率)
        """
        if self.best_model is None:
            raise ValueError("模型未加载，请先加载模型")
        
        # 构建特征向量
        X = pd.DataFrame([features])[self.FEATURE_COLUMNS]
        X = X.fillna(0)
        
        # 标准化
        X_scaled = self.scaler.transform(X)
        
        # 预测
        pred = self.best_model.predict(X_scaled)[0]
        prob = self.best_model.predict_proba(X_scaled)[0, 1] if hasattr(self.best_model, 'predict_proba') else None
        
        return pred, prob
    
    def get_feature_importance(self) -> pd.DataFrame:
        """
        获取特征重要性
        
        Returns:
            特征重要性DataFrame
        """
        if self.best_model is None:
            raise ValueError("模型未加载，请先加载模型")
        
        if hasattr(self.best_model, 'feature_importances_'):
            importance = self.best_model.feature_importances_
        elif hasattr(self.best_model, 'coef_'):
            importance = np.abs(self.best_model.coef_[0])
        else:
            logger.warning("当前模型不支持特征重要性分析")
            return None
        
        df = pd.DataFrame({
            'feature': self.FEATURE_COLUMNS,
            'importance': importance
        }).sort_values('importance', ascending=False)
        
        return df


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='种植计划匹配模型训练脚本')
    parser.add_argument('--data', '-d', type=str, required=True,
                        help='训练数据CSV文件路径')
    parser.add_argument('--output', '-o', type=str, default='./models',
                        help='模型输出目录 (默认: ./models)')
    parser.add_argument('--tune', '-t', action='store_true',
                        help='是否进行超参数调优')
    parser.add_argument('--test-size', type=float, default=0.2,
                        help='测试集比例 (默认: 0.2)')
    
    args = parser.parse_args()
    
    # 检查数据文件是否存在
    if not os.path.exists(args.data):
        logger.error(f"数据文件不存在: {args.data}")
        sys.exit(1)
    
    # 初始化训练器
    trainer = MatchingModelTrainer(output_dir=args.output)
    
    # 加载数据
    df = trainer.load_data(args.data)
    
    # 预处理
    X, y = trainer.preprocess_data(df)
    
    # 检查数据量
    if len(X) < 100:
        logger.warning("数据量较少，可能影响模型效果")
    
    # 训练和评估
    results = trainer.train_and_evaluate(X, y, test_size=args.test_size)
    
    # 超参数调优
    if args.tune:
        best_params = trainer.hyperparameter_tuning(X, y)
        logger.info(f"调优后最佳参数: {best_params}")
    
    # 保存模型
    model_path = trainer.save_model()
    
    # 输出特征重要性
    importance = trainer.get_feature_importance()
    if importance is not None:
        logger.info("\n特征重要性排名:")
        for _, row in importance.iterrows():
            logger.info(f"  {row['feature']}: {row['importance']:.4f}")
    
    # 输出所有模型的评估结果
    logger.info("\n所有模型评估结果:")
    for name, metrics in results.items():
        logger.info(f"\n{name}:")
        logger.info(f"  Accuracy:  {metrics['accuracy']:.4f}")
        logger.info(f"  Precision: {metrics['precision']:.4f}")
        logger.info(f"  Recall:    {metrics['recall']:.4f}")
        logger.info(f"  F1 Score:  {metrics['f1']:.4f}")
        if 'auc' in metrics:
            logger.info(f"  AUC:       {metrics['auc']:.4f}")
    
    logger.info(f"\n训练完成！模型已保存至: {model_path}")


if __name__ == '__main__':
    main()
