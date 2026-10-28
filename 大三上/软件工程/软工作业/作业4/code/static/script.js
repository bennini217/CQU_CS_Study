// 标签页切换
function showTab(tabName) {
    // 隐藏所有标签页
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // 移除所有按钮的active类
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // 显示选中的标签页
    document.getElementById(tabName + '-tab').classList.add('active');
    
    // 激活对应的按钮
    event.target.classList.add('active');
    
    // 如果切换到列表页，加载作品列表
    if (tabName === 'list') {
        loadPaintings();
    }
}

// 文件选择处理
document.getElementById('image').addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        document.getElementById('file-name').textContent = file.name;
        
        // 显示预览
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('image-preview');
            preview.innerHTML = `<img src="${e.target.result}" alt="预览">`;
        };
        reader.readAsDataURL(file);
    }
});

// 拖拽上传
const fileUploadArea = document.getElementById('file-upload-area');
const fileInput = document.getElementById('image');

fileUploadArea.addEventListener('dragover', function(e) {
    e.preventDefault();
    fileUploadArea.style.background = '#e8e8ff';
});

fileUploadArea.addEventListener('dragleave', function(e) {
    e.preventDefault();
    fileUploadArea.style.background = '#f9f9ff';
});

fileUploadArea.addEventListener('drop', function(e) {
    e.preventDefault();
    fileUploadArea.style.background = '#f9f9ff';
    
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        fileInput.files = files;
        const file = files[0];
        document.getElementById('file-name').textContent = file.name;
        
        // 显示预览
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('image-preview');
            preview.innerHTML = `<img src="${e.target.result}" alt="预览">`;
        };
        reader.readAsDataURL(file);
    }
});

fileUploadArea.addEventListener('click', function() {
    fileInput.click();
});

// 表单提交
document.getElementById('register-form').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const formData = new FormData(this);
    const submitBtn = document.querySelector('.submit-btn');
    const resultMessage = document.getElementById('result-message');
    
    // 禁用提交按钮
    submitBtn.disabled = true;
    submitBtn.textContent = '处理中...';
    resultMessage.style.display = 'none';
    
    try {
        const response = await fetch('/api/register', {
            method: 'POST',
            body: formData
        });
        
        const data = await response.json();
        
        if (data.success) {
            resultMessage.className = 'result-message success';
            resultMessage.textContent = '✓ ' + data.message;
            resultMessage.style.display = 'block';
            
            // 清空表单
            this.reset();
            document.getElementById('image-preview').innerHTML = '';
            document.getElementById('file-name').textContent = '点击选择文件或拖拽文件到此处';
        } else {
            resultMessage.className = 'result-message error';
            resultMessage.textContent = '✗ ' + data.message;
            resultMessage.style.display = 'block';
            
            // 如果有相似作品，显示模态框
            if (data.similar_painting) {
                showSimilarModal(data.similar_painting);
            }
        }
    } catch (error) {
        resultMessage.className = 'result-message error';
        resultMessage.textContent = '✗ 网络错误，请稍后重试';
        resultMessage.style.display = 'block';
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '提交登记';
    }
});

// 加载作品列表
async function loadPaintings() {
    const listContainer = document.getElementById('paintings-list');
    listContainer.innerHTML = '<div class="loading">加载中...</div>';
    
    try {
        const response = await fetch('/api/paintings');
        const data = await response.json();
        
        if (data.success) {
            if (data.data.length === 0) {
                listContainer.innerHTML = `
                    <div class="empty-state">
                        <p>📭</p>
                        <p>暂无已登记作品</p>
                    </div>
                `;
            } else {
                listContainer.innerHTML = data.data.map(painting => `
                    <div class="painting-card">
                        <img src="${painting.image_path}" alt="${painting.title}" onerror="this.src='data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'200\' height=\'200\'%3E%3Crect fill=\'%23ddd\' width=\'200\' height=\'200\'/%3E%3Ctext fill=\'%23999\' font-family=\'sans-serif\' font-size=\'14\' dy=\'10.5\' font-weight=\'bold\' x=\'50%25\' y=\'50%25\' text-anchor=\'middle\'%3E图片加载失败%3C/text%3E%3C/svg%3E'">
                        <div class="info">
                            <h3>${escapeHtml(painting.title)}</h3>
                            <p><strong>作者：</strong>${escapeHtml(painting.author)}</p>
                            <p><strong>类型：</strong>${escapeHtml(painting.work_type)}</p>
                            <p><strong>分辨率：</strong>${painting.width} × ${painting.height}</p>
                            <p><strong>登记时间：</strong>${new Date(painting.registration_time).toLocaleString('zh-CN')}</p>
                        </div>
                    </div>
                `).join('');
            }
        } else {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <p>✗ 加载失败：${data.message}</p>
                </div>
            `;
        }
    } catch (error) {
        listContainer.innerHTML = `
            <div class="empty-state">
                <p>✗ 网络错误，请稍后重试</p>
            </div>
        `;
    }
}

// 显示相似作品模态框
function showSimilarModal(similarPainting) {
    const modal = document.getElementById('similar-modal');
    const info = document.getElementById('similar-painting-info');
    
    info.innerHTML = `
        <p><strong>标题：</strong>${escapeHtml(similarPainting.title)}</p>
        <p><strong>作者：</strong>${escapeHtml(similarPainting.author)}</p>
        <p><strong>作品类型：</strong>${escapeHtml(similarPainting.work_type)}</p>
        <p><strong>登记时间：</strong>${new Date(similarPainting.registration_time).toLocaleString('zh-CN')}</p>
        <img src="${similarPainting.image_path}" alt="${similarPainting.title}" onerror="this.style.display='none'">
    `;
    
    modal.style.display = 'block';
}

// 关闭相似作品模态框
function closeSimilarModal() {
    document.getElementById('similar-modal').style.display = 'none';
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('similar-modal');
    if (event.target == modal) {
        modal.style.display = 'none';
    }
}

// HTML转义函数
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}



