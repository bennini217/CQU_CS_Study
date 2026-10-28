import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon
from skimage.color import xyz2lab
from colormath.color_objects import LabColor
from colormath.color_diff import delta_e_cie2000
from scipy.optimize import minimize

plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False 

class ColorConfig:
    """颜色空间配置类"""
    def __init__(self):
        # CIE 1931 光谱轨迹数据
        self.cie1931_x = np.array([
            0.1741,0.1740,0.1738,0.1736,0.1733,0.1730,0.1726,0.1721,
            0.1714,0.1703,0.1689,0.1669,0.1644,0.1611,0.1566,0.1510,
            0.1440,0.1355,0.1241,0.1096,0.0913,0.0687,0.0454,0.0235,
            0.0082,0.0039,0.0139,0.0389,0.0743,0.1142,0.1547,0.1929,
            0.2296,0.2658,0.3016,0.3373,0.3731,0.4087,0.4441,0.4788,
            0.5125,0.5448,0.5752,0.6029,0.6270,0.6482,0.6658,0.6801,
            0.6915,0.7006,0.7079,0.7140,0.7190,0.7230,0.7260,0.7283,
            0.7300,0.7311,0.7320,0.7327,0.7334,0.7340,0.7344,0.7346
        ])
        
        self.cie1931_y = np.array([
            0.0050,0.0050,0.0049,0.0049,0.0048,0.0048,0.0048,0.0048,
            0.0051,0.0058,0.0069,0.0086,0.0109,0.0138,0.0177,0.0227,
            0.0297,0.0399,0.0578,0.0868,0.1327,0.2007,0.2950,0.4127,
            0.5384,0.6548,0.7502,0.8120,0.8338,0.8262,0.8059,0.7816,
            0.7543,0.7243,0.6923,0.6589,0.6245,0.5896,0.5547,0.5202,
            0.4866,0.4544,0.4242,0.3965,0.3725,0.3514,0.3340,0.3197,
            0.3083,0.2993,0.2920,0.2859,0.2809,0.2770,0.2740,0.2717,
            0.2700,0.2689,0.2680,0.2673,0.2666,0.2660,0.2656,0.2654
        ])
        
        # 颜色空间定义
        self.color_spaces = {
            'VideoRGBV': {  # 4通道视频源
                'name': '视频源RGBV',
                'color': 'darkorange',
                'points': np.array([
                    [0.708, 0.292],   # R
                    [0.170, 0.797],   # G
                    [0.131, 0.046],   # B
                    [0.300, 0.600]    # V
                ]),
                'brightness': np.array([0.2126, 0.7152, 0.0722, 0.15])
            },
            'DisplayRGBCX': {  # 5通道显示屏
                'name': 'LED显示屏RGBCX',
                'color': 'dodgerblue',
                'points': np.array([
                    [0.6946, 0.3047],  # R
                    [0.2612, 0.7067],  # G
                    [0.1418, 0.0417],  # B
                    [0.2870, 0.3250],  # C
                    [0.4500, 0.1800]   # X
                ]),
                'brightness': np.array([0.2126, 0.7152, 0.0722, 0.25, 0.12])
            }
        }
        
        # D65 白点 (CIE 1931 XYZ)
        self.d65_XYZ = np.array([95.047, 100.000, 108.883])

# ==================== 核心计算模块 ====================
class MultiChannelColorScience:
    """多通道颜色科学计算核心类"""
    def __init__(self, config):
        self.config = config
        
        # 为每个色彩空间计算转换矩阵
        self.matrices = {}
        for space_name in self.config.color_spaces:
            self.matrices[space_name] = self._compute_matrices(space_name)
    
    def _compute_matrices(self, space_name):
        """计算多通道到XYZ的转换矩阵"""
        space = self.config.color_spaces[space_name]
        points = space['points']
        brightness = space['brightness']
        
        XYZ_values = []
        for i, (x, y) in enumerate(points):
            Y = brightness[i]
            X = (x / y) * Y
            Z = ((1 - x - y) / y) * Y
            XYZ_values.append([X, Y, Z])
        return np.array(XYZ_values)
    
    def convert_4ch_to_5ch(self, rgbv):
        def objective(rgbcx, xyz_target, Y_ref, lambda1, lambda2, lambda3):
            # 计算转换后的XYZ
            xyz_calc = np.zeros(3)
            for j in range(5):
                xyz_calc += rgbcx[j] * self.matrices['DisplayRGBCX'][j]
            Y_map = xyz_calc[1]  # Y分量
            
            # 计算色差Lab_ref和Lab_map
            lab_ref = xyz2lab(xyz_target.reshape(1, -1) / 100.0)[0]
            lab_map = xyz2lab(xyz_calc.reshape(1, -1) / 100.0)[0]
            color1 = LabColor(lab_l=lab_ref[0], lab_a=lab_ref[1], lab_b=lab_ref[2])
            color2 = LabColor(lab_l=lab_map[0], lab_a=lab_map[1], lab_b=lab_map[2])
            delta_e = delta_e_cie2000(color1, color2)
            
            # 亮度差绝对值
            brightness_diff = np.abs(Y_ref - Y_map)
            
            # 负值惩罚项
            negative_penalty = np.sum(np.maximum(0, -rgbcx))
            
            # 总损失函数
            loss = (lambda1 * delta_e + 
                    lambda2 * brightness_diff + 
                    lambda3 * negative_penalty)
            return loss

        # 超参数设置（根据论文推荐值）
        lambda1, lambda2, lambda3 = 0.7, 0.2, 0.1

        single_input = False
        if len(rgbv.shape) == 1:
            rgbv = rgbv.reshape(1, -1)
            single_input = True

        # 计算输入的XYZ值（包括Y_ref）
        src_matrices = self.matrices['VideoRGBV']
        XYZ = np.zeros((rgbv.shape[0], 3))
        for i in range(rgbv.shape[0]):
            for j in range(4):
                XYZ[i] += rgbv[i, j] * src_matrices[j]
        Y_ref_values = XYZ[:, 1]  # 提取Y分量

        # 优化执行
        rgbcx_result = np.zeros((rgbv.shape[0], 5))
        for i in range(rgbv.shape[0]):
            initial_guess = np.zeros(5)
            initial_guess[:3] = rgbv[i, :3]
            initial_guess[3:] = 0.1  # C/X初始值
            
            result = minimize(
                objective, 
                initial_guess, 
                args=(XYZ[i], Y_ref_values[i], lambda1, lambda2, lambda3),
                bounds=[(0, 1)]*5,
                method='L-BFGS-B'
            )
            
            if result.success:
                rgbcx_result[i] = result.x
            else:
                # 失败时回退策略
                rgbcx_result[i, :3] = rgbv[i, :3]
                rgbcx_result[i, 3] = (rgbv[i, 1] + rgbv[i, 2]) / 2
                rgbcx_result[i, 4] = rgbv[i, 3]
        
        rgbcx_result = np.clip(rgbcx_result, 0, 1)
        return rgbcx_result[0] if single_input else rgbcx_result
    
    def compute_color_loss(self, src_rgbv, dst_rgbcx):
        """色差计算模块"""
        single_input = False
        if len(src_rgbv.shape) == 1:
            src_rgbv = src_rgbv.reshape(1, -1)
            dst_rgbcx = dst_rgbcx.reshape(1, -1)
            single_input = True
        
        # XYZ转换
        src_XYZ = np.zeros((src_rgbv.shape[0], 3))
        for i in range(src_rgbv.shape[0]):
            for j in range(4):
                src_XYZ[i] += src_rgbv[i, j] * self.matrices['VideoRGBV'][j]
        
        dst_XYZ = np.zeros((dst_rgbcx.shape[0], 3))
        for i in range(dst_rgbcx.shape[0]):
            for j in range(5):
                dst_XYZ[i] += dst_rgbcx[i, j] * self.matrices['DisplayRGBCX'][j]
        
        # ΔE2000计算
        delta_e_values = []
        src_Lab = xyz2lab(src_XYZ / 100.0)
        dst_Lab = xyz2lab(dst_XYZ / 100.0)
        
        for i in range(src_rgbv.shape[0]):
            color1 = LabColor(lab_l=src_Lab[i, 0], lab_a=src_Lab[i, 1], lab_b=src_Lab[i, 2])
            color2 = LabColor(lab_l=dst_Lab[i, 0], lab_a=dst_Lab[i, 1], lab_b=dst_Lab[i, 2])
            delta_e = delta_e_cie2000(color1, color2)
            delta_e_values.append(delta_e)
        
        return delta_e_values[0] if single_input else np.array(delta_e_values)

# ==================== 可视化模块 ====================        
class MultiChannelVisualizer:
    """多通道颜色空间可视化模块"""
    def __init__(self, config):
        self.config = config
        
    def plot_spectrum(self, ax):
        """绘制光谱轨迹"""
        ax.plot(
            self.config.cie1931_x, self.config.cie1931_y,
            'k-', lw=1, alpha=0.7, label='CIE 1931 光谱轨迹'
        )
        
    def plot_color_gamut(self, ax, space_name):
        """色域绘制修正版"""
        space = self.config.color_spaces[space_name]
        points = space['points']
        
        # 绘制基色点
        ax.scatter(
            points[:, 0], points[:, 1],
            color=space['color'], s=50, alpha=0.8,
            label=f"{space['name']} 基色"
        )
        
        # 多边形绘制逻辑
        n_channels = points.shape[0]
        if n_channels >= 3:
            # 构建多边形顶点
            polygon_points = np.vstack([points, points[0]])  # 闭合处理
            
            # 绘制多边形边界
            ax.plot(
                polygon_points[:,0], polygon_points[:,1],
                '-', color=space['color'], lw=2,
                label=f"{space['name']} 色域"
            )
            ax.add_patch(Polygon(
                points, closed=True, alpha=0.2, color=space['color']
            ))
    
    def visualize_color_spaces(self):
        """色域对比可视化"""
        fig, ax = plt.subplots(figsize=(12, 8))
        self.plot_spectrum(ax)
        
        # 绘制各空间色域
        self.plot_color_gamut(ax, 'VideoRGBV')
        self.plot_color_gamut(ax, 'DisplayRGBCX')
        
        ax.set_xlabel('CIE x', fontsize=12)
        ax.set_ylabel('CIE y', fontsize=12)
        ax.set_xlim(0, 0.8)
        ax.set_ylim(0, 0.9)
        ax.grid(True, alpha=0.3)
        ax.set_title('4通道到5通道色域对比', fontsize=14)
        ax.legend(loc='upper right')
        
        plt.tight_layout()
        plt.savefig('color_gamut_comparison.png', dpi=300)
        print("色域对比图已保存为 color_gamut_comparison.png")
    
    def visualize_color_conversion(self, cs, sample_count=8):
        """转换效果分析"""
        samples = np.linspace(0, 1, sample_count)
        rgb_grid = np.array(np.meshgrid(samples, samples, samples)).T.reshape(-1, 3)
        
        np.random.seed(42)
        v_channel = np.random.uniform(0, 1, size=(rgb_grid.shape[0], 1))
        rgbv_samples = np.hstack([rgb_grid, v_channel])
        
        rgbcx_samples = cs.convert_4ch_to_5ch(rgbv_samples)
        delta_e_values = cs.compute_color_loss(rgbv_samples, rgbcx_samples)
        
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 8))
        
        # 色差分布
        ax1.hist(delta_e_values, bins=30, color='skyblue', edgecolor='black')
        ax1.set_xlabel('ΔE2000 色差')
        ax1.set_ylabel('样本数量')
        ax1.set_title(f'平均ΔE={np.mean(delta_e_values):.2f}')
        ax1.grid(True, alpha=0.3)
        
        # 亮度对比
        sample_indices = np.random.choice(rgbv_samples.shape[0], 20, replace=False)
        x = np.arange(len(sample_indices))
        width = 0.35
        
        rgbv_brightness = np.sum(rgbv_samples[sample_indices, :3] * [0.299, 0.587, 0.114], axis=1)
        rgbcx_brightness = np.sum(rgbcx_samples[sample_indices, :3] * [0.299, 0.587, 0.114], axis=1)
        
        ax2.bar(x - width/2, rgbv_brightness, width, label='RGBV', color='darkorange')
        ax2.bar(x + width/2, rgbcx_brightness, width, label='RGBCX', color='dodgerblue')
        ax2.set_title('亮度对比')
        ax2.legend()
        ax2.grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.savefig('color_conversion_analysis.png', dpi=300)
        print("转换分析图已保存为 color_conversion_analysis.png")
        
        return np.mean(delta_e_values)
    
    def visualize_conversion_effect(self, cs):
        test_colors = [
            [1, 0, 0, 0.5],      # 红
            [0, 1, 0, 0.5],      # 绿
            [0, 0, 1, 0.5],      # 蓝
            [1, 1, 0, 0.5],      # 黄
            [0.5, 0.5, 0.5, 0.5],# 灰
            [0.0, 1.0, 1.0, 0.5],# 青
            [1.0, 0.0, 1.0, 0.5],# 洋红
            [0.2, 0.4, 0.7, 0.5] # 深蓝紫
        ]

        rgbv_array = np.array(test_colors)
        rgbcx_array = cs.convert_4ch_to_5ch(rgbv_array)

        fig, ax = plt.subplots(2, len(test_colors), figsize=(3 * len(test_colors), 5))
        plt.suptitle("颜色转换视觉对比", fontsize=16)

        for i in range(len(test_colors)):
            color_rgbv = np.clip(rgbv_array[i], 0, 1)
            color_rgbcx = np.clip(rgbcx_array[i], 0, 1)

            ax[0, i].imshow([[color_rgbv[:3]]])
            label_rgbv = f"原始\nR={color_rgbv[0]:.2f}\nG={color_rgbv[1]:.2f}\nB={color_rgbv[2]:.2f}\nV={color_rgbv[3]:.2f}"
            ax[0, i].set_title(label_rgbv, fontsize=9)
            ax[0, i].axis('off')

            ax[1, i].imshow([[color_rgbcx[:3]]])
            label_rgbcx = (
                f"转换\nR={color_rgbcx[0]:.2f}\nG={color_rgbcx[1]:.2f}\nB={color_rgbcx[2]:.2f}\n"
                f"C={color_rgbcx[3]:.2f}\nX={color_rgbcx[4]:.2f}"
            )
            ax[1, i].set_title(label_rgbcx, fontsize=9)
            ax[1, i].axis('off')

        plt.tight_layout()
        plt.savefig('visual_comparison.png', dpi=300)
        print("视觉效果对比图已保存为 visual_comparison.png")

# ==================== 主程序 ====================        
def main():
    try:
        config = ColorConfig()
        cs = MultiChannelColorScience(config)
        viz = MultiChannelVisualizer(config)
        
        # 生成所有可视化结果
        viz.visualize_color_spaces()
        viz.visualize_conversion_effect(cs)
        
        # 执行转换示例
        test_rgbv = np.array([
            [1.0, 0.0, 0.0, 0.5],  # 红色
            [0.0, 1.0, 0.0, 0.3],  # 绿色
            [0.0, 0.0, 1.0, 0.7],  # 蓝色
            [1.0, 1.0, 0.0, 0.2],  # 黄色
            [0.0, 1.0, 1.0, 0.6],  # 青色
            [1.0, 0.0, 1.0, 0.4],  # 洋红
            [0.5, 0.5, 0.5, 0.5],  # 灰色
        ])
        
        test_rgbcx = cs.convert_4ch_to_5ch(test_rgbv)
        delta_e_values = cs.compute_color_loss(test_rgbv, test_rgbcx)
        
        print("\n转换示例结果:")
        for i, (src, dst, de) in enumerate(zip(test_rgbv, test_rgbcx, delta_e_values)):
            print(f"样本{i+1}: ΔE={de:.3f}")
            print(f"  原始: {src.round(3)}")
            print(f"  转换: {dst.round(3)}\n")
        
        # 生成分析图表
        viz.visualize_color_conversion(cs)
        
        print("\n所有处理完成！生成文件列表:")
        print("- color_gamut_comparison.png: 色域对比图")
        print("- visual_comparison.png: 视觉效果对比图")
        print("- color_conversion_analysis.png: 转换分析图")

    except Exception as e:
        print(f"程序执行出错: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()