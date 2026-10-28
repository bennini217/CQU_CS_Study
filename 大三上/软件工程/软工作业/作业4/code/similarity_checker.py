"""
相似度检查模块 - 负责计算图像相似度
"""
import imagehash
from typing import List, Tuple, Optional
from painting import Painting


class SimilarityChecker:
    """相似度检查器类"""
    
    def __init__(self, threshold: int = 10):
        """
        初始化相似度检查器
        
        Args:
            threshold: 相似度阈值（汉明距离），低于此值认为相似
        """
        self.threshold = threshold
    
    def calculate_hamming_distance(self, hash1: str, hash2: str) -> int:
        """
        计算两个哈希值之间的汉明距离
        
        Args:
            hash1: 第一个哈希值
            hash2: 第二个哈希值
            
        Returns:
            汉明距离
        """
        try:
            h1 = imagehash.hex_to_hash(hash1)
            h2 = imagehash.hex_to_hash(hash2)
            return h1 - h2  # imagehash库重载了减号运算符，返回汉明距离
        except Exception as e:
            raise ValueError(f"计算汉明距离时出错: {e}")
    
    def find_most_similar(self, target_hash: str, paintings: List[Painting]) -> Optional[Tuple[Painting, int]]:
        """
        在已存证的绘画中找到与目标哈希最相似的绘画
        
        Args:
            target_hash: 目标哈希值
            paintings: 已存证的绘画列表
            
        Returns:
            (最相似的绘画对象, 汉明距离)，如果没有找到则返回None
        """
        if not paintings:
            return None
        
        min_distance = float('inf')
        most_similar = None
        
        for painting in paintings:
            if painting.hash_value:
                distance = self.calculate_hamming_distance(target_hash, painting.hash_value)
                if distance < min_distance:
                    min_distance = distance
                    most_similar = painting
        
        if most_similar is None:
            return None
        
        return most_similar, int(min_distance)
    
    def is_similar(self, hash1: str, hash2: str) -> bool:
        """
        判断两个哈希值是否相似（汉明距离小于阈值）
        
        Args:
            hash1: 第一个哈希值
            hash2: 第二个哈希值
            
        Returns:
            是否相似
        """
        distance = self.calculate_hamming_distance(hash1, hash2)
        return distance < self.threshold



