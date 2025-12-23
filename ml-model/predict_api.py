"""
种子商品匹配模型预测API服务
使用Flask提供REST接口，供Java应用调用

预测接口:
  POST /api/predict - 预测匹配分数
  GET /api/health - 健康检查
  GET /api/model-info - 获取模型信息
"""

import json
import logging
import sys
from pathlib import Path
from functools import lru_cache
import traceback

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import pandas as pd
import numpy as np

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 初始化Flask应用
app = Flask(__name__)
CORS(app)

# 全局变量存储模型
model = None
scaler = None
feature_columns = [
    'variety_score',
    'region_score',
    'climate_score',
    'season_score',
    'quality_score',
    'intent_score'
]


def load_model(model_dir='./models'):
    """
    加载训练好的模型和缩放器
    
    Args:
        model_dir: 模型目录
        
    Returns:
        (model, scaler) 或 (None, None) 如果加载失败
    """
    global model, scaler
    
    try:
        model_path = Path(model_dir) / 'match_model.pkl'
        scaler_path = Path(model_dir) / 'feature_scaler.pkl'
        
        if not model_path.exists() or not scaler_path.exists():
            logger.error(f"Model files not found in {model_dir}")
            return None, None
        
        logger.info(f"Loading model from {model_path}")
        model = joblib.load(model_path)
        
        logger.info(f"Loading scaler from {scaler_path}")
        scaler = joblib.load(scaler_path)
        
        logger.info("Model and scaler loaded successfully")
        return model, scaler
        
    except Exception as e:
        logger.error(f"Error loading model: {e}", exc_info=True)
        return None, None


@app.route('/api/health', methods=['GET'])
def health_check():
    """
    健康检查接口
    
    Returns:
        JSON: 健康状态
    """
    try:
        status = 'healthy' if model is not None and scaler is not None else 'unhealthy'
        return jsonify({
            'status': status,
            'model_loaded': model is not None,
            'scaler_loaded': scaler is not None
        })
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/api/model-info', methods=['GET'])
def model_info():
    """
    获取模型信息
    
    Returns:
        JSON: 模型相关信息
    """
    try:
        if model is None:
            return jsonify({'error': 'Model not loaded'}), 503
        
        return jsonify({
            'model_type': 'RandomForestClassifier',
            'n_estimators': model.n_estimators,
            'max_depth': model.max_depth,
            'features': feature_columns,
            'feature_count': len(feature_columns)
        })
    except Exception as e:
        logger.error(f"Error getting model info: {e}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/predict', methods=['POST'])
def predict():
    """
    预测匹配分数
    
    请求体JSON格式:
    {
        "variety_score": 90.0,
        "region_score": 85.0,
        "climate_score": 88.0,
        "season_score": 90.0,
        "quality_score": 88.0,
        "intent_score": 75.0
    }
    
    或批量预测:
    [
        {"variety_score": 90.0, ...},
        {"variety_score": 85.0, ...}
    ]
    
    Returns:
        JSON: 预测分数(0-100)
    """
    try:
        if model is None or scaler is None:
            logger.error("Model or scaler not loaded")
            return jsonify({'error': 'Model not initialized'}), 503
        
        # 获取请求数据
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        # 处理批量预测
        if isinstance(data, list):
            return _batch_predict(data)
        else:
            return _single_predict(data)
            
    except Exception as e:
        logger.error(f"Prediction error: {e}", exc_info=True)
        return jsonify({
            'error': 'Prediction failed',
            'message': str(e)
        }), 500


def _single_predict(data):
    """
    单条预测
    
    Args:
        data: 输入特征字典
        
    Returns:
        JSON响应
    """
    try:
        # 验证输入
        missing_features = [f for f in feature_columns if f not in data]
        if missing_features:
            return jsonify({
                'error': f'Missing required features: {missing_features}'
            }), 400
        
        # 构建特征向量
        features = np.array([[
            float(data.get(f, 0)) for f in feature_columns
        ]], dtype=np.float32)
        
        # 验证特征值范围
        if np.any(features < 0) or np.any(features > 100):
            return jsonify({
                'error': 'Feature values must be between 0-100'
            }), 400
        
        # 特征标准化
        features_scaled = scaler.transform(features)
        
        # 预测
        proba = model.predict_proba(features_scaled)[0, 1]
        score = float(proba * 100)
        
        # 获取特征贡献度
        feature_importance = {}
        for i, feature in enumerate(feature_columns):
            feature_importance[feature] = {
                'value': float(data.get(feature, 0)),
                'importance': float(model.feature_importances_[i])
            }
        
        logger.info(f"Prediction: score={score:.2f}")
        
        return jsonify({
            'score': round(score, 2),
            'confidence': round(proba, 4),
            'grade': _calculate_grade(score),
            'features': feature_importance
        })
        
    except ValueError as e:
        logger.error(f"Invalid input: {e}")
        return jsonify({'error': f'Invalid input: {str(e)}'}), 400
    except Exception as e:
        logger.error(f"Error in single prediction: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500


def _batch_predict(data_list):
    """
    批量预测
    
    Args:
        data_list: 输入特征字典列表
        
    Returns:
        JSON响应
    """
    try:
        if not isinstance(data_list, list) or len(data_list) == 0:
            return jsonify({'error': 'Invalid input format'}), 400
        
        # 构建特征矩阵
        features_list = []
        for data in data_list:
            missing = [f for f in feature_columns if f not in data]
            if missing:
                return jsonify({
                    'error': f'Missing features in record: {missing}'
                }), 400
            
            features_list.append([
                float(data.get(f, 0)) for f in feature_columns
            ])
        
        features = np.array(features_list, dtype=np.float32)
        
        # 验证特征值范围
        if np.any(features < 0) or np.any(features > 100):
            return jsonify({
                'error': 'Feature values must be between 0-100'
            }), 400
        
        # 特征标准化
        features_scaled = scaler.transform(features)
        
        # 批量预测
        probas = model.predict_proba(features_scaled)[:, 1]
        scores = (probas * 100).round(2)
        
        # 构建结果
        results = []
        for i, (data, score) in enumerate(zip(data_list, scores)):
            results.append({
                'index': i,
                'score': float(score),
                'confidence': float(probas[i]),
                'grade': _calculate_grade(float(score))
            })
        
        logger.info(f"Batch prediction completed: {len(results)} records")
        
        return jsonify({
            'count': len(results),
            'results': results
        })
        
    except Exception as e:
        logger.error(f"Error in batch prediction: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500


def _calculate_grade(score):
    """
    根据分数计算等级
    
    Args:
        score: 分数(0-100)
        
    Returns:
        等级字符串(A/B/C/D)
    """
    if score >= 80:
        return 'A'
    elif score >= 60:
        return 'B'
    elif score >= 40:
        return 'C'
    else:
        return 'D'


@app.errorhandler(404)
def not_found(error):
    """处理404错误"""
    return jsonify({'error': 'Not found'}), 404


@app.errorhandler(500)
def internal_error(error):
    """处理500错误"""
    logger.error(f"Internal error: {error}")
    return jsonify({'error': 'Internal server error'}), 500


def main():
    """
    主函数：启动Flask服务
    """
    import sys
    
    # 获取模型目录
    model_dir = sys.argv[1] if len(sys.argv) > 1 else './models'
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 5000
    
    # 加载模型
    global model, scaler
    model, scaler = load_model(model_dir)
    
    if model is None or scaler is None:
        logger.error("Failed to load model")
        sys.exit(1)
    
    logger.info(f"Starting API server on port {port}")
    logger.info(f"Model directory: {model_dir}")
    
    # 启动Flask服务
    app.run(
        host='0.0.0.0',
        port=port,
        debug=False,
        use_reloader=False
    )


if __name__ == '__main__':
    main()
