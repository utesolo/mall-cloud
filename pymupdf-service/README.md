# PyMuPDF4LLM PDF解析服务

这是一个基于PyMuPDF4LLM的PDF解析服务，专为LLM优化，提供HTTP接口供Java服务调用。

## 功能特性

- PDF文档解析，保留文档结构
- 支持文件上传和URL方式
- 专为LLM优化的文本提取
- RESTful API接口
- 健康检查端点

## 安装依赖

```bash
pip install -r requirements.txt
```

## 运行服务

```bash
python pymupdf_service.py
```

服务将在 `http://0.0.0.0:5000` 启动

## API接口

### 健康检查

```
GET /health
```

### 解析PDF文档

```
POST /parse-pdf
Content-Type: multipart/form-data

参数:
- file: PDF文件
- extract_images: 是否提取图片（可选）
- page_range: 页码范围（可选）
```

或使用JSON格式:

```
POST /parse-pdf
Content-Type: application/json

{
    "file_url": "http://example.com/document.pdf",
    "extract_images": false,
    "page_range": "1-10"
}
```

### 响应格式

```json
{
    "success": true,
    "total_pages": 10,
    "pages": [
        {
            "page_number": 1,
            "content": "页面文本内容...",
            "metadata": {
                "has_images": false,
                "word_count": 500
            }
        }
    ]
}
```

## Docker部署（可选）

```bash
# 构建镜像
docker build -t pymupdf-service .

# 运行容器
docker run -d -p 5000:5000 pymupdf-service
```

## 注意事项

1. 默认最大文件大小为50MB
2. 仅支持PDF格式文件
3. 临时文件会自动清理
4. 建议在生产环境使用Gunicorn等WSGI服务器

## 生产环境部署

使用Gunicorn运行:

```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 pymupdf_service:app
```
