import numpy as np
from PIL import Image
from scipy.stats import chi2
import matplotlib.pyplot as plt
import os

# --- 模块一：LSB 隐写与提取核心 ---
class LSBProcessor:
    @staticmethod
    def text_to_bin(text):
        # 将文本转换为 8 位二进制字符串
        return ''.join(format(ord(c), '08b') for c in text)

    @staticmethod
    def bin_to_text(binary_str):
        # 将二进制串重构为文字信息 
        chars = [chr(int(binary_str[i:i+8], 2)) for i in range(0, len(binary_str), 8)]
        return ''.join(chars)

    def encode(self, img_obj, msg):
        """
        全量向量化 LSB 编码：拒绝慢速 for 循环，直接对 NumPy 矩阵做位运算
        """
        # 直接对传入的图片对象进行 convert
        img = img_obj.convert('RGB')
        img_arr = np.array(img)
        
        # 将信息转为二进制位，并直接化为 numpy 整型数组
        bit_stream = self.text_to_bin(msg)
        bit_len = len(bit_stream)
        bits = np.array(list(bit_stream), dtype=np.uint8)
        
        # 将图像矩阵展平为一维数组，方便直接从头开始按序写入
        flat_img = img_arr.flatten()
        capacity = len(flat_img)
        
        if bit_len > capacity:
            raise IndexError(f"错误：信息长度 ({bit_len}) 超过图像容量 ({capacity})")

        # 【核心优化】：利用底层 C 语言级别的向量化操作
        # 先用 0xFE (254) 抹除前 bit_len 个像素通道的最低位，再与秘密信息做或运算
        flat_img[:bit_len] = (flat_img[:bit_len] & 0xFE) | bits
        
        # 将修改后的一维数组重新塑形回原来的 (H, W, 3) 图像矩阵
        stego_img = Image.fromarray(flat_img.reshape(img_arr.shape))
        return stego_img, bit_len

    def decode(self, stego_img, bit_len):
        """ 全量向量化 LSB 解码 """
        img_arr = np.array(stego_img)
        flat_img = img_arr.flatten()
        
        # 批量提取最低有效位，并转回字符串
        extracted_bits_arr = flat_img[:bit_len] & 1
        extracted_bits = ''.join(extracted_bits_arr.astype(str))
        
        return self.bin_to_text(extracted_bits)

# --- 模块二：卡方检测与直方图分析 ---
class StegoAnalyzer:
    def analyze(self, original_img, stego_img):
        # 直接提取 RGB 三个通道的原始数值进行分析
        org_data = np.array(original_img).flatten()
        stego_data = np.array(stego_img).flatten()

        # 1. 绘制直方图对比 
        plt.figure(figsize=(12, 5))
        plt.subplot(1, 2, 1)
        plt.hist(org_data, bins=256, color='blue', alpha=0.6, label='Original')
        plt.hist(stego_data, bins=256, color='red', alpha=0.4, label='Stego')
        plt.title("RGB Histogram Comparison")
        plt.legend()

        # 2. 计算卡方隐写概率 
        p_stego = self.calculate_chi_probability(stego_data)
        print(f"检测分析完成：图像含隐写信息的概率约为 {p_stego:.2f}% ")
        
        plt.subplot(1, 2, 2)
        plt.text(0.1, 0.5, f"Detected Stego Probability:\n\n{p_stego:.4f}%", fontsize=14)
        plt.axis('off')
        plt.show()

    def calculate_chi_probability(self, pixel_data):
        # 统计 [0, 255] 每个灰阶像素出现的频次
        counts = np.bincount(pixel_data, minlength=256)
        even_counts = counts[0::2]
        odd_counts = counts[1::2]
        
        # 理论期望值：成对像素频率的均值 
        expected = (even_counts + odd_counts) / 2.0
        valid_idx = expected > 0
        
        # 【数学逻辑修正】：严格按照皮尔逊卡方检验标准公式
        # 偶数位和奇数位的偏差都需要累加，否则卡方统计量会少算一半
        chi_stat = np.sum(((even_counts[valid_idx] - expected[valid_idx])**2) / expected[valid_idx]) + \
                   np.sum(((odd_counts[valid_idx] - expected[valid_idx])**2) / expected[valid_idx])
        
        # 自由度 = 有效像素对数量 - 1 
        df = np.sum(valid_idx) - 1
        
        # 返回隐写概率 (1 - 累积分布函数值) 
        return (1 - chi2.cdf(chi_stat, df)) * 100

# --- 主程序运行 ---
if __name__ == "__main__":
    processor = LSBProcessor()
    analyzer = StegoAnalyzer()
    
    SECRET_MSG = "CQUWATERMASKEXP"
    INPUT_IMAGE = "lenna.png"
    OUTPUT_IMAGE = "lsb_lenna.png"
    
    # 模拟环境：如果没有大图，生成一张 8x8 的微型图像。
    # 100% 满载的微型图像能完美验证卡方算法的有效性。
    if not os.path.exists(INPUT_IMAGE):
        print(f"未找到 {INPUT_IMAGE}，正在生成演示用的 8x8 随机噪声图 (用于满载测试)...")
        noise = np.random.randint(0, 256, (8, 8, 3), dtype=np.uint8)
        Image.fromarray(noise).save(INPUT_IMAGE)

    # 强行切出一块 8x8 的子图 (容量: 8 * 8 * 3 = 192 bits)
    # original_img = Image.open(INPUT_IMAGE).crop((0, 0, 8, 8)) 
    original_img = Image.open(INPUT_IMAGE)

    # 1. 执行隐写
    # 在满载率高达 120/192 = 62.5% 的情况下进行隐写
    stego_result, b_len = processor.encode(original_img, SECRET_MSG)
    stego_result.save(OUTPUT_IMAGE)
    print(f"隐写完成！[信息: {SECRET_MSG}, 长度: {b_len} bits]")

    # 2. 执行提取
    extracted_msg = processor.decode(stego_result, b_len)
    print(f"成功提取信息: {extracted_msg}")

    # 3. 统计学分析与绘图
    analyzer.analyze(original_img, stego_result)