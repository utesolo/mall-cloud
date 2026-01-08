#!/usr/bin/env python3
"""
PyMuPDF4LLM PDF解析服务
提供HTTP接口用于解析PDF文档，专为LLM优化

依赖安装:
pip install flask pymupdf4llm requests werkzeug

运行方式:
python pymupdf_service.py

Author: mall-cloud
Version: 1.0.0
"""

from flask import Flask, request, jsonify
import pymupdf4llm
import os
import tempfile
import logging
from werkzeug.utils import secure_filename

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB最大文件大小

# 允许的文件扩展名
ALLOWED_EXTENSIONS = {'pdf'}


def allowed_file(filename):
    """检查文件扩展名是否允许"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'healthy',
        'service': 'pymupdf4llm',
        'version': '1.0.0'
    })


@app.route('/parse-pdf', methods=['POST'])
def parse_pdf():
    """
    解析PDF文档接口
    
    请求方式: multipart/form-data 或 application/json
    
    参数（文件上传方式）:
        - file: PDF文件
        - extract_images: 是否提取图片（可选，默认false）
        - page_range: 页码范围，如"1-10"（可选）
    
    参数（URL方式）:
        - file_url: PDF文件URL
        - extract_images: 是否提取图片（可选）
        - page_range: 页码范围（可选）
    
    返回:
        {
            "success": true,
            "total_pages": 10,
            "pages": [
                {
                    "page_number": 1,
                    "content": "页面文本内容",
                    "metadata": {...}
                }
            ]
        }
    """
    try:
        pdf_path = None
        temp_file = None
        
        # 处理文件上传
        if 'file' in request.files:
            file = request.files['file']
            if file.filename == '':
                return jsonify({'success': False, 'error': '未选择文件'}), 400
            
            if not allowed_file(file.filename):
                return jsonify({'success': False, 'error': '只支持PDF文件'}), 400
            
            # 保存到临时文件
            temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.pdf')
            file.save(temp_file.name)
            pdf_path = temp_file.name
            logger.info(f"接收到文件: {file.filename}, 大小: {os.path.getsize(pdf_path)} bytes")
        
        # 处理JSON请求（URL方式）
        elif request.is_json:
            data = request.get_json()
            file_url = data.get('file_url')
            if not file_url:
                return jsonify({'success': False, 'error': '缺少file_url参数'}), 400
            
            # 下载文件
            import requests
            response = requests.get(file_url, timeout=30)
            response.raise_for_status()
            
            temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.pdf')
            temp_file.write(response.content)
            temp_file.close()
            pdf_path = temp_file.name
            logger.info(f"下载文件: {file_url}, 大小: {len(response.content)} bytes")
        else:
            return jsonify({'success': False, 'error': '请求格式错误'}), 400
        
        # 获取参数
        extract_images = request.form.get('extract_images', 'false').lower() == 'true' if 'file' in request.files else request.get_json().get('extract_images', False)
        page_range = request.form.get('page_range') if 'file' in request.files else request.get_json().get('page_range')
        
        # 使用PyMuPDF4LLM解析PDF
        logger.info(f"开始解析PDF: {pdf_path}")
        md_text = pymupdf4llm.to_markdown(pdf_path)
        
        # 简单分页处理（基于换页符或固定规则）
        pages_content = md_text.split('\n---\n')  # 假设使用---作为页分隔符
        
        # 构建返回结果
        pages = []
        for idx, content in enumerate(pages_content, start=1):
            if content.strip():
                pages.append({
                    'page_number': idx,
                    'content': content.strip(),
                    'metadata': {
                        'has_images': False,  # TODO: 根据实际情况判断
                        'word_count': len(content.split())
                    }
                })
        
        result = {
            'success': True,
            'total_pages': len(pages),
            'pages': pages
        }
        
        logger.info(f"解析完成，共 {len(pages)} 页")
        return jsonify(result)
    
    except Exception as e:
        logger.error(f"解析PDF出错: {str(e)}", exc_info=True)
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
    
    finally:
        # 清理临时文件
        if pdf_path and os.path.exists(pdf_path):
            try:
                os.unlink(pdf_path)
                logger.info(f"清理临时文件: {pdf_path}")
            except Exception as e:
                logger.warning(f"清理临时文件失败: {e}")


if __name__ == '__main__':
    logger.info("启动PyMuPDF服务...")
    logger.info("服务地址: http://0.0.0.0:5000")
    logger.info("健康检查: http://0.0.0.0:5000/health")
    logger.info("解析接口: http://0.0.0.0:5000/parse-pdf")
    
    app.run(host='0.0.0.0', port=5000, debug=False)
