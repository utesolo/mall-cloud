#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
匹配模型推理服务

提供REST API用于实时匹配预测

作者: mall-cloud
版本: 1.0.0
"""

import os
import logging
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import joblib
import pandas as pd

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 创建FastAPI应用
app = FastAPI(
    title="种植计划匹配预测服务",
    description="基于机器学习的种植计划与商品匹配预测API",
    version="1.0.0"
)

# 特征列名
FEATURE_COLUMNS = [
    'variety_score',
    'region_score',
    'climate_score',
    'season_score',
    'quality_score',
    'intent_score'
]

# 全局变量存储模型和标准化器
model = None
scaler = None


class MatchFeatures(BaseModel):
    """匹配特征请求体"""
    variety_score: float = Field(ge=0, le=100, description="品种一致性得分")
    region_score: float = Field(ge=0, le=100, description="区域适配得分")
    climate_score: float = Field(ge=0, le=100, description="气候匹配得分")
    season_score: float = Field(ge=0, le=100, description="季节匹配得分")
    quality_score: float = Field(ge=0, le=100, description="种子质量得分")
    intent_score: float = Field(ge=0, le=100, description="供需意图得分")
    
    class Config:
        json_schema_extra = {
            "example": {
                "variety_score": 85.0,
                "region_score": 90.0,
                "climate_score": 78.5,
                "season_score": 100.0,
                "quality_score": 82.0,
                "intent_score": 75.0
            }
        }


class PredictionResponse(BaseModel):
    """预测响应体"""
    is_match: bool = Field(description="是否匹配")
    confidence: float = Field(description="置信度 (0-1)")
    match_grade: str = Field(description="匹配等级 (A/B/C/D)")
    recommendation: str = Field(description="匹配建议")


class BatchPredictionRequest(BaseModel):
    """批量预测请求体"""
    features_list: list[MatchFeatures]


class BatchPredictionResponse(BaseModel):
    """批量预测响应体"""
    predictions: list[PredictionResponse]


def load_model_and_scaler(model_path: str, scaler_path: str):
    """加载模型和标准化器"""
    global model, scaler
    
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"模型文件不存在: {model_path}")
    if not os.path.exists(scaler_path):
        raise FileNotFoundError(f"标准化器文件不存在: {scaler_path}")
    
    logger.info(f"加载模型: {model_path}")
    model = joblib.load(model_path)
    
    logger.info(f"加载标准化器: {scaler_path}")
    scaler = joblib.load(scaler_path)
    
    logger.info("模型加载完成")


def predict_single(features: MatchFeatures) -> PredictionResponse:
    """单个预测"""
    if model is None or scaler is None:
        raise HTTPException(status_code=500, detail="模型未加载")
    
    # 构建特征向量
    X = pd.DataFrame([{
        'variety_score': features.variety_score,
        'region_score': features.region_score,
        'climate_score': features.climate_score,
        'season_score': features.season_score,
        'quality_score': features.quality_score,
        'intent_score': features.intent_score
    }])[FEATURE_COLUMNS]
    
    # 标准化
    X_scaled = scaler.transform(X)
    
    # 预测
    pred = model.predict(X_scaled)[0]
    prob = model.predict_proba(X_scaled)[0, 1] if hasattr(model, 'predict_proba') else 0.5
    
    # 计算综合得分
    total_score = (
        features.variety_score * 0.25 +
        features.region_score * 0.20 +
        features.climate_score * 0.15 +
        features.season_score * 0.15 +
        features.quality_score * 0.15 +
        features.intent_score * 0.10
    )
    
    # 确定匹配等级
    if total_score >= 80:
        grade = 'A'
    elif total_score >= 60:
        grade = 'B'
    elif total_score >= 40:
        grade = 'C'
    else:
        grade = 'D'
    
    # 生成建议
    if prob >= 0.7:
        recommendation = "强烈推荐，匹配度很高"
    elif prob >= 0.5:
        recommendation = "建议选用，匹配度良好"
    elif prob >= 0.3:
        recommendation = "可作为备选，匹配度一般"
    else:
        recommendation = "不推荐，匹配度较低"
    
    return PredictionResponse(
        is_match=bool(pred),
        confidence=round(float(prob), 4),
        match_grade=grade,
        recommendation=recommendation
    )


@app.on_event("startup")
async def startup_event():
    """应用启动时加载模型"""
    model_path = os.environ.get('MODEL_PATH', './models/latest_model.pkl')
    scaler_path = os.environ.get('SCALER_PATH', './models/latest_scaler.pkl')
    
    try:
        load_model_and_scaler(model_path, scaler_path)
    except FileNotFoundError as e:
        logger.warning(f"模型文件未找到: {e}")
        logger.warning("请通过 /admin/load-model 接口手动加载模型")


@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "model_loaded": model is not None
    }


@app.post("/predict", response_model=PredictionResponse)
async def predict(features: MatchFeatures):
    """
    单个匹配预测
    
    根据输入的6维特征，预测种植计划与商品是否匹配
    """
    return predict_single(features)


@app.post("/predict/batch", response_model=BatchPredictionResponse)
async def predict_batch(request: BatchPredictionRequest):
    """
    批量匹配预测
    
    批量预测多个种植计划与商品的匹配度
    """
    predictions = [predict_single(f) for f in request.features_list]
    return BatchPredictionResponse(predictions=predictions)


@app.post("/admin/load-model")
async def admin_load_model(model_path: str, scaler_path: str):
    """
    管理接口：加载模型
    
    手动加载指定路径的模型文件
    """
    try:
        load_model_and_scaler(model_path, scaler_path)
        return {"status": "success", "message": "模型加载成功"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8090)
