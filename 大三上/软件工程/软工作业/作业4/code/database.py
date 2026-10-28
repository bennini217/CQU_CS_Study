"""
数据存储模块 - 负责绘画数据的持久化存储
"""
import json
import os
from typing import List, Optional
from painting import Painting


class Database:
    """数据库类 - 使用JSON文件存储数据"""
    
    def __init__(self, db_file: str = "paintings_db.json"):
        """
        初始化数据库
        
        Args:
            db_file: 数据库文件路径
        """
        self.db_file = db_file
        self._ensure_db_file()
    
    def _ensure_db_file(self):
        """确保数据库文件存在"""
        if not os.path.exists(self.db_file):
            with open(self.db_file, 'w', encoding='utf-8') as f:
                json.dump([], f)
    
    def load_all(self) -> List[Painting]:
        """
        加载所有已存证的绘画
        
        Returns:
            绘画列表
        """
        try:
            with open(self.db_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                return [Painting.from_dict(item) for item in data]
        except Exception as e:
            print(f"加载数据时出错: {e}")
            return []
    
    def save(self, painting: Painting) -> bool:
        """
        保存绘画到数据库
        
        Args:
            painting: 绘画对象
            
        Returns:
            是否保存成功
        """
        try:
            paintings = self.load_all()
            # 分配ID
            if painting.painting_id is None:
                max_id = max([p.painting_id for p in paintings if p.painting_id], default=0)
                painting.painting_id = max_id + 1
            
            paintings.append(painting)
            
            with open(self.db_file, 'w', encoding='utf-8') as f:
                json.dump([p.to_dict() for p in paintings], f, ensure_ascii=False, indent=2)
            
            return True
        except Exception as e:
            print(f"保存数据时出错: {e}")
            return False
    
    def get_count(self) -> int:
        """获取已存证绘画数量"""
        return len(self.load_all())



