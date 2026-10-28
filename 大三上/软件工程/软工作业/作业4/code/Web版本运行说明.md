# 作品版权登记系统 - Web版本运行说明

## 📋 系统架构

这是一个**前后端分离的Web应用系统**：

- **后端**：Flask (Python Web框架)
  - 提供RESTful API接口
  - 处理文件上传
  - 执行业务逻辑（分辨率检查、哈希生成、相似度比对）

- **前端**：HTML + CSS + JavaScript
  - 响应式Web界面
  - 文件上传和预览
  - 异步数据交互

## 🚀 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

这会安装：
- Flask - Web框架
- flask-cors - 跨域支持
- Pillow - 图像处理
- imagehash - 图像哈希算法
- numpy - 数值计算

### 2. 启动服务器

```bash
python app.py
```

服务器将在 `http://localhost:5000` 启动

### 3. 访问系统

在浏览器中打开：`http://localhost:5000`

## 📁 文件结构说明

### 后端文件

- **app.py** - Flask后端主程序
  - 定义API路由
  - 处理文件上传
  - 调用业务逻辑

- **copyright_registration_system.py** - 业务逻辑核心（保持不变）
- **painting.py** - 数据模型（保持不变）
- **image_processor.py** - 图像处理（保持不变）
- **similarity_checker.py** - 相似度计算（保持不变）
- **database.py** - 数据存储（保持不变）

### 前端文件

- **templates/index.html** - 主页面HTML
  - 登记表单
  - 作品列表展示
  - 相似作品模态框

- **static/style.css** - 样式文件
  - 响应式设计
  - 现代化UI界面

- **static/script.js** - 前端JavaScript
  - 表单提交
  - API调用
  - 动态内容更新

### 目录结构

```
.
├── app.py                          # Flask后端主程序
├── templates/
│   └── index.html                  # 前端HTML页面
├── static/
│   ├── style.css                   # 样式文件
│   └── script.js                   # JavaScript文件
├── uploads/                        # 上传文件存储目录（自动创建）
├── paintings_db.json               # 数据库文件（自动创建）
├── copyright_registration_system.py # 业务逻辑
├── painting.py                     # 数据模型
├── image_processor.py              # 图像处理
├── similarity_checker.py           # 相似度检查
├── database.py                     # 数据存储
└── requirements.txt                # 依赖包列表
```

## 🎯 功能说明

### 1. 登记绘画版权

1. 在"登记版权"标签页填写信息：
   - 作品标题
   - 作者
   - 作品类型（下拉选择）
   - 上传图像文件

2. 点击"提交登记"按钮

3. 系统自动：
   - 检查图像分辨率（≥800×600）
   - 生成pHash特征值
   - 与已存证作品比对相似度
   - 决定是否登记

4. 显示结果：
   - 成功：显示成功消息
   - 失败：显示错误原因
   - 如果发现相似作品，弹出模态框显示相似作品信息

### 2. 查看已登记作品

1. 点击"已登记作品"标签页
2. 系统自动加载并显示所有已登记的作品
3. 每个作品卡片显示：
   - 作品缩略图
   - 标题、作者、类型
   - 分辨率
   - 登记时间

## 🔧 API接口说明

### GET /api/paintings
获取所有已登记的作品

**响应示例：**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "我的画作",
      "author": "张三",
      "work_type": "油画",
      "registration_time": "2024-01-01T10:00:00",
      "image_path": "/uploads/20240101_100000_painting.jpg",
      "width": 1200,
      "height": 800
    }
  ]
}
```

### POST /api/register
登记绘画版权

**请求格式：** multipart/form-data

**参数：**
- `title`: 作品标题（必填）
- `author`: 作者（必填）
- `work_type`: 作品类型（必填）
- `image`: 图像文件（必填）

**成功响应：**
```json
{
  "success": true,
  "message": "版权登记成功！作品ID: 1"
}
```

**失败响应（分辨率不足）：**
```json
{
  "success": false,
  "message": "图像分辨率不足！要求至少800*600，当前为600*400"
}
```

**失败响应（相似度过高）：**
```json
{
  "success": false,
  "message": "该作品与已存证作品相似度过高（汉明距离: 0），拒绝登记！",
  "similar_painting": {
    "id": 1,
    "title": "已存证作品",
    "author": "李四",
    "work_type": "油画",
    "registration_time": "2024-01-01T09:00:00",
    "image_path": "/uploads/20240101_090000_existing.jpg"
  }
}
```

## 🎨 前端特性

1. **响应式设计**：适配不同屏幕尺寸
2. **文件拖拽上传**：支持点击选择或拖拽上传
3. **图像预览**：上传前可预览图像
4. **实时反馈**：提交后立即显示处理结果
5. **模态框展示**：相似作品以模态框形式展示
6. **现代化UI**：渐变背景、卡片式布局、动画效果

## 📝 使用示例

### 示例1：成功登记

1. 填写表单：
   - 标题：我的第一幅画
   - 作者：张三
   - 类型：油画
   - 上传：选择800×600或更大的图像

2. 点击"提交登记"

3. 显示：✓ 版权登记成功！作品ID: 1

### 示例2：分辨率不足

1. 上传小于800×600的图像

2. 显示：✗ 图像分辨率不足！要求至少800*600，当前为600*400

### 示例3：重复作品

1. 上传与已存证作品相同的图像

2. 显示：✗ 该作品与已存证作品相似度过高（汉明距离: 0），拒绝登记！

3. 弹出模态框显示相似作品信息

## 🔍 技术细节

### 后端技术栈
- **Flask 2.3+** - 轻量级Web框架
- **Werkzeug** - 文件上传处理
- **flask-cors** - 跨域资源共享

### 前端技术栈
- **原生JavaScript** - 无框架依赖
- **Fetch API** - 异步HTTP请求
- **CSS3** - 现代化样式

### 业务逻辑
- 保持原有业务逻辑不变
- 所有核心功能（分辨率检查、pHash生成、相似度比对）完全复用

## ⚠️ 注意事项

1. **文件大小限制**：最大16MB
2. **支持格式**：PNG、JPG、JPEG、GIF、BMP
3. **上传目录**：文件保存在 `uploads/` 目录
4. **数据存储**：使用JSON文件存储（`paintings_db.json`）
5. **端口**：默认5000，可在 `app.py` 中修改

## 🐛 常见问题

**Q: 启动后无法访问？**
A: 检查端口5000是否被占用，或修改 `app.py` 中的端口号

**Q: 上传文件失败？**
A: 检查文件大小是否超过16MB，格式是否支持

**Q: 图片无法显示？**
A: 检查 `uploads/` 目录是否存在，文件是否成功上传

**Q: 跨域错误？**
A: 已配置CORS，如仍有问题，检查浏览器控制台错误信息





