"""
绘画类 - 表示一个绘画作品及其基本信息
"""
from datetime import datetime
from typing import Optional


class Painting:
    """绘画作品类"""
    
    def __init__(self, title: str, author: str, registration_time: datetime, 
                 work_type: str, image_path: str, width: int = 0, height: int = 0,
                 hash_value: Optional[str] = None, painting_id: Optional[int] = None):
        """
        初始化绘画对象
        
        Args:
            title: 标题
            author: 作者
            registration_time: 登记时间
            work_type: 作品类型
            image_path: 图像路径
            width: 图像宽度
            height: 图像高度
            hash_value: 哈希特征值
            painting_id: 绘画ID（用于数据库存储）
        """
        self.painting_id = painting_id
        self.title = title
        self.author = author
        self.registration_time = registration_time
        self.work_type = work_type
        self.image_path = image_path
        self.width = width
        self.height = height
        self.hash_value = hash_value
    
    def to_dict(self) -> dict:
        """转换为字典格式（用于存储）"""
        return {
            'id': self.painting_id,
            'title': self.title,
            'author': self.author,
            'registration_time': self.registration_time.isoformat(),
            'work_type': self.work_type,
            'image_path': self.image_path,
            'width': self.width,
            'height': self.height,
            'hash_value': self.hash_value
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Painting':
        """从字典创建绘画对象"""
        return cls(
            painting_id=data.get('id'),
            title=data['title'],
            author=data['author'],
            registration_time=datetime.fromisoformat(data['registration_time']),
            work_type=data['work_type'],
            image_path=data['image_path'],
            width=data['width'],
            height=data['height'],
            hash_value=data.get('hash_value')
        )
    
    def __str__(self):
        return f"Painting(title='{self.title}', author='{self.author}', type='{self.work_type}')"



