"""
Flask后端API - 作品版权登记系统
"""
from flask import Flask, request, jsonify, render_template, send_from_directory
from flask_cors import CORS
from datetime import datetime
import os
from werkzeug.utils import secure_filename
from copyright_registration_system import CopyrightRegistrationSystem

app = Flask(__name__)
CORS(app)  # 允许跨域请求

# 配置
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 最大16MB
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}

# 确保上传目录存在
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# 初始化系统
system = CopyrightRegistrationSystem(similarity_threshold=10, db_file="paintings_db.json")


def allowed_file(filename):
    """检查文件扩展名是否允许"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/')
def index():
    """首页"""
    return render_template('index.html')


@app.route('/api/paintings', methods=['GET'])
def get_paintings():
    """获取所有已登记的作品"""
    try:
        paintings = system.get_all_paintings()
        result = []
        for painting in paintings:
            result.append({
                'id': painting.painting_id,
                'title': painting.title,
                'author': painting.author,
                'registration_time': painting.registration_time.isoformat(),
                'work_type': painting.work_type,
                'image_path': f'/uploads/{os.path.basename(painting.image_path)}',
                'width': painting.width,
                'height': painting.height
            })
        return jsonify({'success': True, 'data': result})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/register', methods=['POST'])
def register_painting():
    """登记绘画版权"""
    try:
        # 检查是否有文件
        if 'image' not in request.files:
            return jsonify({'success': False, 'message': '请上传图像文件'}), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({'success': False, 'message': '未选择文件'}), 400
        
        if not allowed_file(file.filename):
            return jsonify({'success': False, 'message': '不支持的文件格式，请上传图片文件（PNG、JPG等）'}), 400
        
        # 获取表单数据
        title = request.form.get('title', '').strip()
        author = request.form.get('author', '').strip()
        work_type = request.form.get('work_type', '').strip()
        
        if not title or not author or not work_type:
            return jsonify({'success': False, 'message': '请填写完整的作品信息'}), 400
        
        # 保存上传的文件
        filename = secure_filename(file.filename)
        # 添加时间戳避免文件名冲突
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S_')
        filename = timestamp + filename
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        
        # 调用系统进行登记
        registration_time = datetime.now()
        success, message, similar_painting = system.register_painting(
            title=title,
            author=author,
            registration_time=registration_time,
            work_type=work_type,
            image_path=filepath
        )
        
        if success:
            return jsonify({
                'success': True,
                'message': message
            })
        else:
            # 如果失败，删除上传的文件
            if os.path.exists(filepath):
                os.remove(filepath)
            
            # 准备相似作品信息
            similar_info = None
            if similar_painting:
                similar_info = {
                    'id': similar_painting.painting_id,
                    'title': similar_painting.title,
                    'author': similar_painting.author,
                    'work_type': similar_painting.work_type,
                    'registration_time': similar_painting.registration_time.isoformat(),
                    'image_path': f'/uploads/{os.path.basename(similar_painting.image_path)}'
                }
            
            return jsonify({
                'success': False,
                'message': message,
                'similar_painting': similar_info
            }), 400
    
    except Exception as e:
        return jsonify({'success': False, 'message': f'服务器错误: {str(e)}'}), 500


@app.route('/uploads/<filename>')
def uploaded_file(filename):
    """提供上传文件的访问"""
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)



