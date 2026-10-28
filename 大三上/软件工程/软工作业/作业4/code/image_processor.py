"""
图像处理模块 - 负责图像分辨率检查和特征值生成
"""
from PIL import Image
import imagehash
from typing import Tuple, Optional


class ImageProcessor:
    """图像处理器类"""
    
    MIN_WIDTH = 800
    MIN_HEIGHT = 600
    
    @staticmethod
    def check_resolution(image_path: str) -> Tuple[bool, int, int]:
        """
        检查图像分辨率是否符合要求（至少800*600）
        
        Args:
            image_path: 图像文件路径
            
        Returns:
            (是否符合要求, 宽度, 高度)
        """
        try:
            with Image.open(image_path) as img:
                width, height = img.size
                is_valid = width >= ImageProcessor.MIN_WIDTH and height >= ImageProcessor.MIN_HEIGHT
                return is_valid, width, height
        except Exception as e:
            raise ValueError(f"无法打开图像文件: {e}")
    
    @staticmethod
    def generate_hash(image_path: str) -> str:
        """
        使用Perceptual Hash (pHash)算法生成图像特征值
        
        Args:
            image_path: 图像文件路径
            
        Returns:
            哈希特征值（字符串格式）
        """
        try:
            with Image.open(image_path) as img:
                # 使用imagehash库的phash函数生成感知哈希
                hash_value = imagehash.phash(img)
                return str(hash_value)
        except Exception as e:
            raise ValueError(f"无法生成图像哈希值: {e}")



