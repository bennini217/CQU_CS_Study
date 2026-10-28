"""
版权登记系统主类
"""
from datetime import datetime
from typing import Tuple, Optional
from painting import Painting
from image_processor import ImageProcessor
from similarity_checker import SimilarityChecker
from database import Database


class CopyrightRegistrationSystem:
    """版权登记系统类"""
    
    def __init__(self, similarity_threshold: int = 10, db_file: str = "paintings_db.json"):
        """
        初始化版权登记系统
        
        Args:
            similarity_threshold: 相似度阈值（汉明距离）
            db_file: 数据库文件路径
        """
        self.image_processor = ImageProcessor()
        self.similarity_checker = SimilarityChecker(threshold=similarity_threshold)
        self.database = Database(db_file=db_file)
    
    def register_painting(self, title: str, author: str, registration_time: datetime,
                         work_type: str, image_path: str) -> Tuple[bool, str, Optional[Painting]]:
        """
        登记绘画版权
        
        Args:
            title: 标题
            author: 作者
            registration_time: 登记时间
            work_type: 作品类型
            image_path: 图像文件路径
            
        Returns:
            (是否登记成功, 消息, 最相似的绘画对象（如果失败）)
        """
        # 1. 检查分辨率
        try:
            is_valid, width, height = self.image_processor.check_resolution(image_path)
            if not is_valid:
                return False, f"图像分辨率不足！要求至少{ImageProcessor.MIN_WIDTH}*{ImageProcessor.MIN_HEIGHT}，当前为{width}*{height}", None
        except Exception as e:
            return False, f"检查分辨率时出错: {e}", None
        
        # 2. 生成特征值
        try:
            hash_value = self.image_processor.generate_hash(image_path)
        except Exception as e:
            return False, f"生成特征值时出错: {e}", None
        
        # 3. 与已存证绘画进行比对
        existing_paintings = self.database.load_all()
        most_similar = self.similarity_checker.find_most_similar(hash_value, existing_paintings)
        
        if most_similar is not None:
            similar_painting, distance = most_similar
            # 检查是否相似度低于阈值（即距离小于阈值）
            if distance < self.similarity_checker.threshold:
                return False, f"该作品与已存证作品相似度过高（汉明距离: {distance}），拒绝登记！", similar_painting
        
        # 4. 进行版权登记
        new_painting = Painting(
            title=title,
            author=author,
            registration_time=registration_time,
            work_type=work_type,
            image_path=image_path,
            width=width,
            height=height,
            hash_value=hash_value
        )
        
        if self.database.save(new_painting):
            return True, f"版权登记成功！作品ID: {new_painting.painting_id}", None
        else:
            return False, "保存到数据库时出错", None
    
    def get_all_paintings(self):
        """获取所有已存证的绘画"""
        return self.database.load_all()



