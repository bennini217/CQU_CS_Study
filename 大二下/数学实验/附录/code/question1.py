import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon
from skimage.color import xyz2lab
from scipy.optimize import minimize
import matplotlib.gridspec as gridspec

# 修复中文显示问题
plt.rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans', 'Arial Unicode MS', 'WenQuanYi Micro Hei']
plt.rcParams['axes.unicode_minus'] = False

# 添加对旧版 numpy 的兼容性支持
if not hasattr(np, 'asscalar'):
    np.asscalar = lambda a: a.item()

# 手动实现 Delta E 2000 算法
def delta_e_cie2000_manual(lab1, lab2):
    """手动实现 CIE Delta E 2000 色差计算"""
    L1, a1, b1 = lab1
    L2, a2, b2 = lab2
    
    # 计算平均值
    L_bar = (L1 + L2) / 2
    
    # 计算 C 值
    C1 = np.sqrt(a1**2 + b1**2)
    C2 = np.sqrt(a2**2 + b2**2)
    C_bar = (C1 + C2) / 2
    
    # 计算 a' 值
    G = 0.5 * (1 - np.sqrt(C_bar**7 / (C_bar**7 + 25**7)))
    a1_prime = (1 + G) * a1
    a2_prime = (1 + G) * a2
    
    # 计算 C' 和 h' 值
    C1_prime = np.sqrt(a1_prime**2 + b1**2)
    C2_prime = np.sqrt(a2_prime**2 + b2**2)
    C_bar_prime = (C1_prime + C2_prime) / 2
    
    h1_prime = np.degrees(np.arctan2(b1, a1_prime)) % 360
    h2_prime = np.degrees(np.arctan2(b2, a2_prime)) % 360
    
    # 计算 Δh'
    delta_h_prime = h2_prime - h1_prime
    if abs(delta_h_prime) > 180:
        if h2_prime > h1_prime:
            delta_h_prime -= 360
        else:
            delta_h_prime += 360
    
    # 计算 H' 平均值
    if abs(h1_prime - h2_prime) <= 180:
        H_bar_prime = (h1_prime + h2_prime) / 2
    else:
        H_bar_prime = (h1_prime + h2_prime + 360) / 2
    
    # 计算各种差值
    delta_L_prime = L2 - L1
    delta_C_prime = C2_prime - C1_prime
    delta_H_prime = 2 * np.sqrt(C1_prime * C2_prime) * np.sin(np.radians(delta_h_prime / 2))
    
    # 计算 SL, SC, SH
    SL = 1 + (0.015 * (L_bar - 50)**2) / np.sqrt(20 + (L_bar - 50)**2)
    SC = 1 + 0.045 * C_bar_prime
    T = 1 - 0.17 * np.cos(np.radians(H_bar_prime - 30)) + \
        0.24 * np.cos(np.radians(2 * H_bar_prime)) + \
        0.32 * np.cos(np.radians(3 * H_bar_prime + 6)) - \
        0.20 * np.cos(np.radians(4 * H_bar_prime - 63))
    SH = 1 + 0.015 * C_bar_prime * T
    
    # 计算 RT
    delta_theta = 30 * np.exp(-((H_bar_prime - 275) / 25)**2)
    RC = 2 * np.sqrt(C_bar_prime**7 / (C_bar_prime**7 + 25**7))
    RT = -np.sin(np.radians(2 * delta_theta)) * RC
    
    # 考虑标准观察者和照明体的kL, kC, kH（通常为1）
    kL = kC = kH = 1
    
    # 计算最终的 Delta E 2000
    delta_E = np.sqrt(
        (delta_L_prime / (kL * SL))**2 +
        (delta_C_prime / (kC * SC))**2 +
        (delta_H_prime / (kH * SH))**2 +
        RT * (delta_C_prime / (kC * SC)) * (delta_H_prime / (kH * SH))
    )
    
    return delta_E

# ==================== 全局配置 ====================
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
            'BT2020': {
                'name': 'BT.2020',
                'color': 'brown',
                'points': np.array([
                    [0.708, 0.292],  # Red primary
                    [0.170, 0.797],  # Green primary
                    [0.131, 0.046]   # Blue primary
                ])
            },
            'Display': {
                'name': 'Display RGB',
                'color': 'red',
                'points': np.array([
                    [0.6946, 0.3047],  # 更符合普通显示器的red
                    [0.2612, 0.7067],  # 更符合普通显示器的green
                    [0.1418, 0.0417]   # 更符合普通显示器的blue
                ])
            }
        }
        
        # D65 白点 (标准照明体D65)
        self.d65_XYZ = np.array([95.047, 100.000, 108.883])

# ==================== 核心计算模块 ====================
class ColorScience:
    """颜色科学计算核心类"""
    def __init__(self, config):
        self.config = config
        self._validate_inputs()
        
        # 计算转换矩阵
        self.matrices = {
            'BT2020': self._compute_matrix('BT2020'),
            'Display': self._compute_matrix('Display')
        }
        
        # 逆矩阵计算
        self.inverse_matrices = {
            'BT2020': np.linalg.inv(self.matrices['BT2020']),
            'Display': np.linalg.inv(self.matrices['Display'])
        }
        
        # 生成训练数据
        self.training_data = self._generate_training_data()
        
        # 训练优化的颜色映射模型
        self.model = self._train_optimized_mapping()
    
    def _validate_inputs(self):
        """验证输入数据有效性"""
        for space in self.config.color_spaces.values():
            if not isinstance(space['points'], np.ndarray) or space['points'].shape != (3,2):
                raise ValueError(f"Invalid points format for {space['name']}")
                
        if self.config.d65_XYZ.shape != (3,):
            raise ValueError("D65 XYZ must be a 3-element array")
    
    def _compute_matrix(self, space_name):
        """计算RGB到XYZ的转换矩阵"""
        space = self.config.color_spaces[space_name]
        points = space['points']
        
        # 计算每个基色的XYZ值
        XYZ_matrix = []
        for (x, y) in points:
            # 从色度坐标计算XYZ (假设Y=1归一化)
            Y = 1.0
            X = (x / y) * Y
            Z = ((1 - x - y) / y) * Y
            XYZ_matrix.append([X, Y, Z])
        
        # 转换为numpy数组
        XYZ_matrix = np.array(XYZ_matrix).T
        
        # 计算白点适配的比例因子
        try:
            S = np.linalg.solve(XYZ_matrix, self.config.d65_XYZ)
        except np.linalg.LinAlgError:
            raise ValueError(f"Cannot compute white point adaptation for {space['name']}")
        
        # 构建最终的RGB到XYZ转换矩阵
        conversion_matrix = XYZ_matrix @ np.diag(S)
        
        return conversion_matrix
    
    def _generate_training_data(self, num_samples=1000):
        """生成用于训练的RGB颜色样本"""
        # 使用分层采样确保覆盖整个色域
        np.random.seed(42)  # 确保可重现性
        
        # 生成随机RGB样本
        rgb_samples = np.random.uniform(0, 1, (num_samples, 3))
        
        # 添加一些特殊点（边界、顶点等）
        special_points = np.array([
            [0, 0, 0],     # 黑色
            [1, 1, 1],     # 白色
            [1, 0, 0],     # 红色
            [0, 1, 0],     # 绿色
            [0, 0, 1],     # 蓝色
            [1, 1, 0],     # 黄色
            [0, 1, 1],     # 青色
            [1, 0, 1],     # 洋红
            [0.5, 0.5, 0.5] # 灰色
        ])
        
        rgb_samples = np.vstack([rgb_samples, special_points])
        
        return rgb_samples
    
    def get_XYZ(self, space_name, rgb):
        """获取指定颜色空间的XYZ值"""
        if space_name not in self.matrices:
            raise ValueError(f"Unsupported color space: {space_name}")
            
        # 确保输入是一维数组（单个颜色）
        if rgb.ndim > 1:
            rgb = rgb.flatten()
            
        return np.dot(self.matrices[space_name], rgb)
    
    def rgb_to_lab(self, space_name, rgb):
        """将RGB转换为Lab空间"""
        # 先转为XYZ
        xyz = self.get_XYZ(space_name, rgb)
        
        # 然后转为Lab，注意xyz2lab需要归一化的XYZ输入
        lab = xyz2lab(xyz.reshape(1, 3) / 100.0)
        return lab[0]
            
    def compute_delta_e2000(self, lab1, lab2):
        """计算两个Lab颜色间的CIEDE2000色差"""
        return delta_e_cie2000_manual(lab1, lab2)
            
    def compute_color_difference(self, bt2020_rgb, display_rgb):
        """计算BT.2020颜色和Display颜色之间的知觉差异（Delta E 2000）"""
        # 转换到Lab空间
        bt2020_lab = self.rgb_to_lab('BT2020', bt2020_rgb)
        display_lab = self.rgb_to_lab('Display', display_rgb)
        
        # 计算色差
        return self.compute_delta_e2000(bt2020_lab, display_lab)
    
    def _train_optimized_mapping(self):
        """使用CIEDE2000作为损失函数训练颜色映射模型"""
        print("Training color mapping model with CIEDE2000 loss...")
        
        # 使用线性变换作为初始参数
        initial_matrix = np.linalg.inv(self.matrices['Display']) @ self.matrices['BT2020']
        initial_params = initial_matrix.flatten()
        
        # 定义损失函数
        def delta_e_loss(params):
            # 将参数重塑为3x3矩阵
            mapping_matrix = params.reshape(3, 3)
            
            # 计算平均色差
            total_loss = 0.0
            for i, rgb in enumerate(self.training_data):
                # 计算原始颜色的XYZ
                src_XYZ = self.get_XYZ('BT2020', rgb)
                
                # 应用映射矩阵
                mapped_XYZ = np.dot(mapping_matrix, src_XYZ)
                
                # 转换回RGB
                try:
                    dst_rgb = np.linalg.inv(self.matrices['Display']) @ mapped_XYZ
                except np.linalg.LinAlgError:
                    return 1e6  # 惩罚无效解
                
                # 裁剪到有效范围
                dst_rgb = np.clip(dst_rgb, 0, 1)
                
                # 计算色差
                delta_e = self.compute_color_difference(rgb, dst_rgb)
                total_loss += delta_e
                
            # 返回平均色差
            return total_loss / len(self.training_data)
        
        # 优化映射矩阵
        result = minimize(
            delta_e_loss,
            initial_params,
            method='L-BFGS-B',
            options={'maxiter': 500, 'ftol': 1e-6, 'disp': True}
        )
        
        # 保存优化结果
        self.optimal_mapping_matrix = result.x.reshape(3, 3)
        print(f"Optimization completed, final loss: {result.fun:.4f}")
        
        # 打印优化后的矩阵
        print("\nOptimized Matrix:")
        print(self.optimal_mapping_matrix)
        
        return result
    
    def convert_color(self, src_rgb, method='optimized'):
        """
        颜色空间转换
        :param src_rgb: 源RGB值 (0-1范围)
        :param method: 映射方法 ('optimized', 'linear')
        :return: 目标RGB值
        """
        # 确保输入是一维数组
        if src_rgb.ndim > 1:
            src_rgb = src_rgb.flatten()
        
        if method == 'optimized':
            # 使用优化后的映射矩阵
            src_XYZ = self.get_XYZ('BT2020', src_rgb)
            mapped_XYZ = self.optimal_mapping_matrix @ src_XYZ
            dst_rgb = np.linalg.inv(self.matrices['Display']) @ mapped_XYZ
            
        elif method == 'linear':
            # 线性映射（Bradford适配）
            src_XYZ = self.get_XYZ('BT2020', src_rgb)
            dst_XYZ = self._bradford_adaptation(src_XYZ)
            dst_rgb = np.linalg.inv(self.matrices['Display']) @ dst_XYZ
            
        else:
            raise ValueError("Unsupported mapping method. Use 'optimized' or 'linear'.")
            
        # 裁剪到有效范围并返回
        return np.clip(dst_rgb, 0, 1)
    
    def _bradford_adaptation(self, XYZ):
        """Bradford色彩适配算法"""
        # Bradford适配矩阵
        bradford_matrix = np.array([
            [ 0.8951,  0.2664, -0.1614],
            [-0.7502,  1.7135,  0.0367],
            [ 0.0389, -0.0685,  1.0296]
        ])
        
        bradford_inv = np.linalg.inv(bradford_matrix)
        
        # 源白点和目标白点 (假设都是D65)
        src_white = self.config.d65_XYZ
        dst_white = self.config.d65_XYZ
        
        # 转换到Bradford空间
        src_white_bradford = bradford_matrix @ src_white
        
        # 计算适配因子
        adaptation_factors = dst_white / src_white_bradford
        
        # 应用适配
        XYZ_bradford = bradford_matrix @ XYZ
        adapted_XYZ_bradford = XYZ_bradford * adaptation_factors
        
        # 转换回XYZ空间
        adapted_XYZ = bradford_inv @ adapted_XYZ_bradford
        
        return adapted_XYZ
    
    def is_in_gamut(self, src_rgb, target_space='Display'):
        """判断颜色是否在目标色域内"""
        if target_space != 'Display':
            raise ValueError("仅支持Display色域检查")
            
        # 使用线性变换（不裁剪）计算目标RGB
        bt2020_to_display = np.linalg.inv(self.matrices['Display']) @ self.matrices['BT2020']
        target_rgb = bt2020_to_display @ src_rgb
        
        # 检查是否所有分量都在[0,1]范围内（允许微小浮点误差）
        return np.all((target_rgb >= -1e-8) & (target_rgb <= 1 + 1e-8))

# ==================== 可视化模块 ====================        
class Visualizer:
    """可视化模块"""
    def __init__(self, config):
        self.config = config
        
    def plot_spectrum(self, ax):
        """绘制CIE 1931光谱轨迹"""
        ax.plot(
            self.config.cie1931_x, self.config.cie1931_y,
            'k-', lw=1, alpha=0.7, label='CIE 1931 光谱轨迹'
        )
        
    def plot_gamut(self, ax, space_name):
        """绘制指定色彩空间的色域"""
        space = self.config.color_spaces[space_name]
        points = space['points']
        
        # 闭合多边形
        closed_points = np.vstack([points, points[0]])
        
        ax.plot(
            closed_points[:,0], closed_points[:,1],
            '-', color=space['color'], lw=2,
            label=f"{space['name']} 色域"
        )
        ax.add_patch(Polygon(
            points, alpha=0.2, color=space['color']
        ))
        
    def plot_mapping(self, ax, src_points, dst_points, sample_step=50, color='k'):
        """绘制颜色映射关系"""
        # 绘制采样点
        ax.scatter(
            src_points[:,0], src_points[:,1],
            c='orange', s=15, alpha=0.6,
            label='BT.2020 颜色' if sample_step == 50 else None
        )
        ax.scatter(
            dst_points[:,0], dst_points[:,1],
            c=color, s=15, alpha=0.6,
            label=f'{color.upper()} 映射' if sample_step == 50 else None
        )
        
        # 绘制连接线（只显示部分，避免过于密集）
        indices = np.arange(0, len(src_points), sample_step)
        for i in indices:
            ax.plot(
                [src_points[i,0], dst_points[i,0]],
                [src_points[i,1], dst_points[i,1]],
                '--', alpha=0.2, lw=0.5, color=color
            )
    
    def create_figure(self):
        """创建标准化的绘图画布"""
        fig, ax = plt.subplots(figsize=(12, 8))
        ax.set_xlabel('CIE x', fontsize=12)
        ax.set_ylabel('CIE y', fontsize=12)
        ax.set_xlim(-0.1, 0.8)
        ax.set_ylim(-0.1, 0.9)
        ax.grid(True, alpha=0.3, linestyle='--')
        ax.legend(loc='upper right', fontsize=10)
        return fig, ax
    
    def create_comparison_plots(self, cs):
        """创建包含色域内外颜色的对比图"""
        # 生成测试颜色（10个色域内，10个色域外）
        np.random.seed(42)
        test_colors = []
        
        # 生成色域内颜色
        while len(test_colors) < 10:
            rgb = np.random.uniform(0, 1, 3)
            if cs.is_in_gamut(rgb):
                test_colors.append(rgb)
        
        # 生成色域外颜色
        while len(test_colors) < 20:
            rgb = np.random.uniform(0, 1, 3)
            if not cs.is_in_gamut(rgb):
                test_colors.append(rgb)
        
        test_colors = np.array(test_colors)
        
        # 分类标记色域内外
        is_in_gamut = np.array([cs.is_in_gamut(c) for c in test_colors])
        color_categories = ['色域内' if in_gamut else '色域外' for in_gamut in is_in_gamut]
        
        # 生成映射结果
        methods = ['optimized', 'linear']
        method_names = ['优化映射', '线性映射']
        method_colors = ['#1f77b4', '#2ca02c']
        
        results = {}
        for idx, method in enumerate(methods):
            mapped_colors = np.array([cs.convert_color(color, method) for color in test_colors])
            delta_es = []
            
            for i, original in enumerate(test_colors):
                # 计算原始颜色在BT.2020空间的Lab值
                lab_bt2020 = cs.rgb_to_lab('BT2020', original)
                
                # 计算映射后颜色在Display空间的Lab值
                lab_display = cs.rgb_to_lab('Display', mapped_colors[i])
                
                # 计算色差
                delta_e = cs.compute_delta_e2000(lab_bt2020, lab_display)
                delta_es.append(delta_e)
            
            delta_es = np.array(delta_es)
            
            # 分色域统计
            in_gamut_delta = delta_es[is_in_gamut]
            out_gamut_delta = delta_es[~is_in_gamut]
            
            results[method] = {
                'mapped': mapped_colors,
                'delta_e': delta_es,
                'in_gamut_mean': np.mean(in_gamut_delta) if in_gamut_delta.size else 0,
                'out_gamut_mean': np.mean(out_gamut_delta) if out_gamut_delta.size else 0,
                'color': method_colors[idx]
            }
        
        # 创建对比图
        fig = plt.figure(figsize=(14, 22))
        gs = gridspec.GridSpec(len(test_colors) + 1, len(methods) + 2, height_ratios=[1] + [4]*len(test_colors))
        
        # 标题行
        ax_title = plt.subplot(gs[0, 0])
        ax_title.text(0.5, 0.5, "原始颜色", ha='center', va='center', fontsize=14, fontweight='bold')
        ax_title.axis('off')
        
        ax_category = plt.subplot(gs[0, 1])
        ax_category.text(0.5, 0.5, "类别", ha='center', va='center', fontsize=12)
        ax_category.axis('off')
        
        for j, name in enumerate(method_names):
            ax = plt.subplot(gs[0, j+2])
            ax.text(0.5, 0.5, name, ha='center', va='center', fontsize=12, fontweight='bold', color=method_colors[j])
            ax.axis('off')
        
        # 绘制颜色样本
        for i, (color, category) in enumerate(zip(test_colors, color_categories)):
            # 原始颜色
            ax_orig = plt.subplot(gs[i+1, 0])
            ax_orig.add_patch(plt.Rectangle((0, 0), 1, 1, color=color))
            ax_orig.set_title(f'颜色 {i+1}', fontsize=10)
            ax_orig.set_xticks([])
            ax_orig.set_yticks([])
            
            # 类别标记
            ax_cat = plt.subplot(gs[i+1, 1])
            ax_cat.text(0.5, 0.5, category, ha='center', va='center', 
                      fontsize=10, color='green' if category=='色域内' else 'red')
            ax_cat.axis('off')
            
            # 映射结果
            for j, method in enumerate(methods):
                mapped = results[method]['mapped'][i]
                delta_e = results[method]['delta_e'][i]
                color = results[method]['color']
                
                ax = plt.subplot(gs[i+1, j+2])
                ax.add_patch(plt.Rectangle((0, 0), 1, 1, color=mapped))
                
                # 色差显示
                color_quality = 'green' if delta_e < 2 else ('orange' if delta_e < 5 else 'red')
                ax.text(0.5, 1.1, f"ΔE: {delta_e:.2f}", 
                       ha='center', va='center', fontsize=10, color=color_quality)
                
                ax.set_xticks([])
                ax.set_yticks([])
        
        # 统计信息面板
        stats_gs = gridspec.GridSpecFromSubplotSpec(1, len(methods), subplot_spec=gs[-1, :])
        for j, method in enumerate(methods):
            ax_stats = plt.subplot(stats_gs[j])
            stats = results[method]
            
            ax_stats.text(0.05, 0.8, f"{method_names[j]} 统计:", fontsize=12, fontweight='bold')
            ax_stats.text(0.05, 0.6, f"色域内平均: {stats['in_gamut_mean']:.2f}", fontsize=10)
            ax_stats.text(0.05, 0.4, f"色域外平均: {stats['out_gamut_mean']:.2f}", fontsize=10)
            
            ax_stats.axis('off')
        
        plt.suptitle("不同色域位置颜色的映射性能对比", fontsize=16, y=0.98)
        plt.tight_layout(rect=[0, 0.02, 1, 0.96])
        
        return fig, results

    def create_chromaticity_plot(self, cs):
        """创建色域和映射关系的色度图"""
        fig, ax = self.create_figure()
        
        # 绘制光谱轨迹
        self.plot_spectrum(ax)
        
        # 绘制两个色彩空间的色域
        for space_name in cs.config.color_spaces:
            self.plot_gamut(ax, space_name)
            
        # 添加D65白点
        d65_xy = np.array([0.3127, 0.3290])  # D65标准白点
        ax.scatter(d65_xy[0], d65_xy[1], c='gray', s=50, marker='*', label='D65 白点')
        
        # 生成测试样本（使用训练数据的抽样）
        sample_step = 50
        samples = cs.training_data[::sample_step]
        
        # 转换原始颜色到CIE xy
        src_xy = []
        for rgb in samples:
            xyz = cs.get_XYZ('BT2020', rgb)
            total = np.sum(xyz)
            src_xy.append([xyz[0]/total, xyz[1]/total] if total != 0 else [0, 0])
        src_xy = np.array(src_xy)
        
        # 转换不同方法的映射结果到CIE xy
        methods = ['optimized', 'linear']
        method_colors = ['#1f77b4', '#2ca02c']
        dst_xy = {}
        
        for method, color in zip(methods, method_colors):
            dst_rgb = np.array([cs.convert_color(rgb, method) for rgb in samples])
            xy_list = []
            for rgb in dst_rgb:
                xyz = cs.get_XYZ('Display', rgb)
                total = np.sum(xyz)
                xy_list.append([xyz[0]/total, xyz[1]/total] if total != 0 else [0, 0])
            dst_xy[method] = np.array(xy_list)
            self.plot_mapping(ax, src_xy, dst_xy[method], sample_step, color)
        
        # 添加图例说明
        handles, labels = ax.get_legend_handles_labels()
        custom_handles = [
            plt.Line2D([], [], marker='o', color=color, label=name, markersize=8, linestyle='none')
            for name, color in zip(['优化映射', '线性映射'], method_colors)
        ]
        ax.legend(handles=custom_handles + handles[:3], loc='upper right', fontsize=10)
        
        ax.set_title('色彩空间映射关系对比', fontsize=14, pad=20)
        return fig

    def compare_delta_e_distribution(self, cs, samples=1000):
        """比较不同映射方法的色差分布并计算准确率"""
        np.random.seed(42)
        methods = ['optimized', 'linear']
        method_names = ['优化映射', '线性映射']
        method_colors = ['#1f77b4', '#2ca02c']
        
        # 生成测试颜色（包含色域内外）
        test_colors = []
        while len(test_colors) < samples:
            rgb = np.random.uniform(0, 1, 3)
            test_colors.append(rgb)
        test_colors = np.array(test_colors)
        
        # 计算每种方法的色差
        delta_e_results = {method: [] for method in methods}
        
        for color in test_colors:
            lab_original = cs.rgb_to_lab('BT2020', color)
            
            for method in methods:
                mapped_color = cs.convert_color(color, method)
                lab_mapped = cs.rgb_to_lab('Display', mapped_color)
                delta_e = cs.compute_delta_e2000(lab_original, lab_mapped)
                delta_e_results[method].append(delta_e)
        
        # 计算准确率 (ΔE < 2.3 的比例)
        accuracy_results = {}
        for method in methods:
            delta_e_values = np.array(delta_e_results[method])
            accuracy = np.mean(delta_e_values < 2.3)
            avg_delta_e = np.mean(delta_e_values)
            
            accuracy_results[method] = {
                'avg_delta_e': avg_delta_e,
                'accuracy': accuracy
            }
        
        # 创建直方图比较
        fig, ax = plt.subplots(figsize=(12, 6))
        
        for i, method in enumerate(methods):
            ax.hist(
                delta_e_results[method],
                bins=30,
                alpha=0.5,
                color=method_colors[i],
                label=f"{method_names[i]} (平均ΔE: {accuracy_results[method]['avg_delta_e']:.2f}, 准确率: {accuracy_results[method]['accuracy']*100:.1f}%)"
            )
        
        # 添加不可察觉阈值参考线
        ax.axvline(2.3, color='k', linestyle='--', alpha=0.7, label='视觉不可察觉阈值 (ΔE=2.3)')
        
        ax.set_xlabel('ΔE 2000 色差')
        ax.set_ylabel('样本数量')
        ax.set_title('不同映射方法的色差分布对比')
        ax.legend()
        
        plt.tight_layout()
        return fig, delta_e_results, accuracy_results

# ==================== 主程序 ====================
if __name__ == "__main__":
    # 创建配置、计算和可视化实例
    config = ColorConfig()
    cs = ColorScience(config)
    vis = Visualizer(config)
    
    # 创建色度图
    fig_chromaticity = vis.create_chromaticity_plot(cs)
    
    # 创建颜色对比图
    fig_comparison, _ = vis.create_comparison_plots(cs)
    
    # 创建色差分布图
    fig_delta_e, _, accuracy = vis.compare_delta_e_distribution(cs)
    
    # 输出量化结果
    print("\n=== 量化准确率结果 ===")
    for method, data in accuracy.items():
        method_name = "优化映射" if method == "optimized" else "线性映射"
        print(f"{method_name}:")
        print(f"  平均ΔE: {data['avg_delta_e']:.2f}")
        print(f"  准确率 (ΔE<2.3): {data['accuracy']*100:.1f}%")
        print()
    
    # 显示所有图形
    plt.show()