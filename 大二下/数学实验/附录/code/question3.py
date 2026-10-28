import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

class LEDColorCorrectionSimplified:
    def __init__(self, excel_file_path):
        """
        初始化LED颜色校正类，专注于转换矩阵、驱动值计算和可视化。

        参数:
            excel_file_path: 包含数据的Excel文件路径。
        """
        self.excel_file_path = excel_file_path
        self.data = {}
        self.pixel_matrices = {}
        self.driver_values = {}
        self.target_value = 220  # R、G、B的目标显示值
        self.uncorrected_images = {}
        self.corrected_images = {}
        
        # 根据图片提供的显示屏参数
        self.display_params = {
            'white_point': {'x': 0.314, 'y': 0.329},  # 白点，色温6485K
            'red_primary': {'x': 0.683, 'y': 0.324},  # 红基色
            'green_primary': {'x': 0.265, 'y': 0.691},  # 绿基色
            'blue_primary': {'x': 0.150, 'y': 0.060},  # 蓝基色
            'gamma': 2.2  # Gamma值
        }

        # 构建显示屏的 RGB 到 XYZ 转换矩阵
        self.rgb_to_xyz_matrix = self.build_rgb_to_xyz_matrix()

    def build_rgb_to_xyz_matrix(self):
        """构建显示屏的 RGB 到 XYZ 转换矩阵。"""
        # 基色 chromaticity 坐标
        xr, yr = self.display_params['red_primary']['x'], self.display_params['red_primary']['y']
        xg, yg = self.display_params['green_primary']['x'], self.display_params['green_primary']['y']
        xb, yb = self.display_params['blue_primary']['x'], self.display_params['blue_primary']['y']
        xw, yw = self.display_params['white_point']['x'], self.display_params['white_point']['y']

        # 计算 z 值
        zr = 1 - xr - yr
        zg = 1 - xg - yg
        zb = 1 - xb - yb
        zw = 1 - xw - yw

        # 假设 Y=1，计算 XYZ
        Xr = xr / yr
        Yr = 1
        Zr = zr / yr

        Xg = xg / yg
        Yg = 1
        Zg = zg / yg

        Xb = xb / yb
        Yb = 1
        Zb = zb / yb

        Xw = xw / yw
        Yw = 1
        Zw = zw / yw

        # 构建矩阵
        S = np.array([
            [Xr, Xg, Xb],
            [Yr, Yg, Yb],
            [Zr, Zg, Zb]
        ])

        # 白点 XYZ
        W = np.array([Xw, Yw, Zw])

        # 求解缩放因子
        weights = np.linalg.solve(S, W)
        Sr, Sg, Sb = weights

        # 最终 RGB 到 XYZ 矩阵
        rgb_to_xyz = np.array([
            [Sr * Xr, Sg * Xg, Sb * Xb],
            [Sr * Yr, Sg * Yg, Sb * Yb],
            [Sr * Zr, Sg * Zg, Sb * Zb]
        ])

        return rgb_to_xyz

    def rgb_to_xyz(self, rgb):
        """将 RGB 值转换为 CIE XYZ。"""
        rgb_linear = (rgb / 255.0) ** self.display_params['gamma']  # 线性化
        xyz = self.rgb_to_xyz_matrix @ rgb_linear
        return xyz

    def xyz_to_xyy(self, xyz):
        """将 CIE XYZ 转换为 CIE xyY。"""
        X, Y, Z = xyz
        if X + Y + Z == 0:
            return 0, 0, 0
        x = X / (X + Y + Z)
        y = Y / (X + Y + Z)
        return x, y, Y

    def load_data(self):
        """从Excel文件中加载数据。"""
        print("正在加载数据...")
        try:
            excel_data = pd.read_excel(self.excel_file_path, sheet_name=None, header=None)
            print(f"找到 {len(excel_data)} 个工作表")

            target_sheet = None
            component_sheets = {}
            for sheet_name in excel_data.keys():
                sheet_lower = sheet_name.lower()
                if '目标' in sheet_name or 'sheet1' in sheet_lower:
                    target_sheet = sheet_name
                elif 'R_R' in sheet_name:
                    component_sheets['R_R'] = sheet_name
                elif 'R_G' in sheet_name:
                    component_sheets['R_G'] = sheet_name
                elif 'R_B' in sheet_name:
                    component_sheets['R_B'] = sheet_name
                elif 'G_R' in sheet_name:
                    component_sheets['G_R'] = sheet_name
                elif 'G_G' in sheet_name:
                    component_sheets['G_G'] = sheet_name
                elif 'G_B' in sheet_name:
                    component_sheets['G_B'] = sheet_name
                elif 'B_R' in sheet_name:
                    component_sheets['B_R'] = sheet_name
                elif 'B_G' in sheet_name:
                    component_sheets['B_G'] = sheet_name
                elif 'B_B' in sheet_name:
                    component_sheets['B_B'] = sheet_name

            required_components = ['R_R', 'R_G', 'R_B', 'G_R', 'G_G', 'G_B', 'B_R', 'B_G', 'B_B']
            missing = [comp for comp in required_components if comp not in component_sheets]
            if missing:
                print(f"错误：缺少必要分量表: {missing}")
                return False
            if target_sheet is None:
                print("错误：未找到目标值表")
                return False

            target_df = excel_data[target_sheet]
            self.target_value = float(target_df.iloc[0, 0]) if target_df.shape[0] > 0 else 220
            print(f"目标值: {self.target_value}")

            for comp_name, sheet_name in component_sheets.items():
                df = excel_data[sheet_name]
                data_array = df.iloc[:64, :64].values.astype(float)
                data_array = np.nan_to_num(data_array, nan=0.0)
                data_array = np.clip(data_array, 0, 255)
                print(f"{comp_name} 数据范围: 最小值={data_array.min():.2f}, 最大值={data_array.max():.2f}")
                if np.any(np.isnan(data_array)):
                    print(f"警告：{comp_name} 数据中包含 nan 值")
                self.data[comp_name] = data_array
                print(f"加载 {comp_name} 数据: 形状 {data_array.shape}")
            return True

        except Exception as e:
            print(f"数据加载错误: {e}")
            return False

    def build_pixel_matrices(self):
        """为每个像素构建转换矩阵（驱动值到实际显示值）。"""
        print("正在构建像素转换矩阵...")
        required_components = ['R_R', 'R_G', 'R_B', 'G_R', 'G_G', 'G_B', 'B_R', 'B_G', 'B_B']
        for comp in required_components:
            if comp not in self.data:
                print(f"缺少数据组件: {comp}")
                return False

        for i in range(64):
            for j in range(64):
                matrix = np.array([
                    [self.data['R_R'][i, j], self.data['G_R'][i, j], self.data['B_R'][i, j]],
                    [self.data['R_G'][i, j], self.data['G_G'][i, j], self.data['B_G'][i, j]],
                    [self.data['R_B'][i, j], self.data['G_B'][i, j], self.data['B_B'][i, j]]
                ])
                if np.any(np.isnan(matrix)):
                    print(f"像素 ({i}, {j}) 的矩阵包含 nan 值")
                self.pixel_matrices[(i, j)] = matrix
        print("像素转换矩阵构建完成")
        return True

    def calculate_driver_values(self):
        """计算达到目标显示值所需的驱动值，考虑Gamma校正。"""
        print("正在计算驱动值...")
        self.driver_values = {'red': {}, 'green': {}, 'blue': {}}
        gamma = self.display_params['gamma']
        
        targets = {
            'red': np.array([self.target_value, 0, 0]),
            'green': np.array([0, self.target_value, 0]),
            'blue': np.array([0, 0, self.target_value])
        }

        for i in range(64):
            for j in range(64):
                matrix = self.pixel_matrices[(i, j)]
                det = np.linalg.det(matrix)
                if det != 0 and not np.any(np.isnan(matrix)):
                    inv_matrix = np.linalg.inv(matrix)
                else:
                    print(f"像素 ({i}, {j}) 处的矩阵不可逆（行列式={det:.2e}），使用伪逆")
                    inv_matrix = np.linalg.pinv(matrix)

                for color in ['red', 'green', 'blue']:
                    target = targets[color]
                    target_linear = (target / 255.0) ** gamma * 255.0
                    if i == 0 and j == 0:
                        print(f"{color} 目标值 (线性空间, 像素(0,0)): {target_linear}")
                    driver = inv_matrix @ target_linear
                    if np.any(np.isnan(driver)):
                        print(f"像素 ({i}, {j}) 的 {color} 驱动值包含 nan，逆矩阵: {inv_matrix}")
                    driver = np.clip(driver, 0, 255)
                    self.driver_values[color][(i, j)] = driver

                    if i == 0 and j == 0:
                        print(f"{color} 驱动值 (像素(0,0)): {driver}")

        for color in ['red', 'green', 'blue']:
            values = np.array([self.driver_values[color][(i, j)] for i in range(64) for j in range(64)])
            print(f"{color} 驱动值: R∈[{np.nanmin(values[:, 0]):.1f},{np.nanmax(values[:, 0]):.1f}], "
                  f"G∈[{np.nanmin(values[:, 1]):.1f},{np.nanmax(values[:, 1]):.1f}], "
                  f"B∈[{np.nanmin(values[:, 2]):.1f},{np.nanmax(values[:, 2]):.1f}]")
        return True

    def simulate_displays(self):
        """模拟校正前后的显示效果。"""
        print("正在模拟显示效果...")

        self.uncorrected_images = {}
        red_uncorrected = np.zeros((64, 64, 3))
        green_uncorrected = np.zeros((64, 64, 3))
        blue_uncorrected = np.zeros((64, 64, 3))

        for i in range(64):
            for j in range(64):
                red_uncorrected[i, j] = [self.data['R_R'][i, j], self.data['R_G'][i, j], self.data['R_B'][i, j]]
                green_uncorrected[i, j] = [self.data['G_R'][i, j], self.data['G_G'][i, j], self.data['G_B'][i, j]]
                blue_uncorrected[i, j] = [self.data['B_R'][i, j], self.data['B_G'][i, j], self.data['B_B'][i, j]]

        self.uncorrected_images['red'] = np.clip(red_uncorrected, 0, 255).astype(np.uint8)
        self.uncorrected_images['green'] = np.clip(green_uncorrected, 0, 255).astype(np.uint8)
        self.uncorrected_images['blue'] = np.clip(blue_uncorrected, 0, 255).astype(np.uint8)

        print(f"校正前图像 (red) 范围: {self.uncorrected_images['red'].min()} - {self.uncorrected_images['red'].max()}")
        print(f"校正前图像 (green) 范围: {self.uncorrected_images['green'].min()} - {self.uncorrected_images['green'].max()}")
        print(f"校正前图像 (blue) 范围: {self.uncorrected_images['blue'].min()} - {self.uncorrected_images['blue'].max()}")

        self.corrected_images = {}
        for color in ['red', 'green', 'blue']:
            corrected_image = np.zeros((64, 64, 3))
            for i in range(64):
                for j in range(64):
                    driver = self.driver_values[color][(i, j)]
                    matrix = self.pixel_matrices[(i, j)]
                    output = matrix @ driver
                    corrected_image[i, j] = output

                    if i == 0 and j == 0:
                        print(f"{color} 校正后输出 (像素(0,0)): {output}")

            self.corrected_images[color] = np.clip(corrected_image, 0, 255).astype(np.uint8)

        print(f"校正后图像 (red) 范围: {self.corrected_images['red'].min()} - {self.corrected_images['red'].max()}")
        print(f"校正后图像 (green) 范围: {self.corrected_images['green'].min()} - {self.corrected_images['green'].max()}")
        print(f"校正后图像 (blue) 范围: {self.corrected_images['blue'].min()} - {self.corrected_images['blue'].max()}")

        print("显示效果模拟完成")
        return True

    def calculate_loss(self):
        """定义损失函数，结合 CIE xyY 色度误差、亮度误差和均匀性误差。"""
        print("正在计算校正前后的损失...")

        def compute_loss(image, target_rgb):
            # 将目标 RGB 转换为 CIE xyY
            target_xyz = self.rgb_to_xyz(target_rgb)
            target_x, target_y, target_Y = self.xyz_to_xyy(target_xyz)

            # 转换图像到 CIE xyY
            xyy_image = np.zeros((64, 64, 3))
            for i in range(64):
                for j in range(64):
                    xyz = self.rgb_to_xyz(image[i, j])
                    x, y, Y = self.xyz_to_xyy(xyz)
                    xyy_image[i, j] = [x, y, Y]

            # 色度误差（xy 欧氏距离）
            chromaticity_error = np.mean(np.sqrt((xyy_image[:, :, 0] - target_x) ** 2 + (xyy_image[:, :, 1] - target_y) ** 2))

            # 亮度误差（Y 的绝对差）
            luminance_error = np.mean(np.abs(xyy_image[:, :, 2] - target_Y))

            # 均匀性误差（基于 RGB 空间的相邻像素差异）
            uniformity_error = 0
            for i in range(63):
                for j in range(63):
                    diff_right = np.linalg.norm(image[i, j] - image[i, j + 1])
                    diff_down = np.linalg.norm(image[i, j] - image[i + 1, j])
                    uniformity_error += (diff_right + diff_down)
            uniformity_error /= (63 * 63 * 2)

            # 总损失：加权组合
            alpha = 0.4  # 色度误差权重
            beta = 0.3   # 亮度误差权重
            gamma = 0.3  # 均匀性误差权重
            total_loss = alpha * chromaticity_error + beta * luminance_error + gamma * uniformity_error
            return total_loss, chromaticity_error, luminance_error, uniformity_error

        targets = {
            'red': np.array([self.target_value, 0, 0]),
            'green': np.array([0, self.target_value, 0]),
            'blue': np.array([0, 0, self.target_value])
        }

        losses = {'uncorrected': {}, 'corrected': {}}
        for color in ['red', 'green', 'blue']:
            uncorrected_image = self.uncorrected_images[color].astype(float)
            total_loss, chromaticity_error, luminance_error, uniformity_error = compute_loss(uncorrected_image, targets[color])
            losses['uncorrected'][color] = {
                'total_loss': total_loss,
                'chromaticity_error': chromaticity_error,
                'luminance_error': luminance_error,
                'uniformity_error': uniformity_error
            }

            corrected_image = self.corrected_images[color].astype(float)
            total_loss, chromaticity_error, luminance_error, uniformity_error = compute_loss(corrected_image, targets[color])
            losses['corrected'][color] = {
                'total_loss': total_loss,
                'chromaticity_error': chromaticity_error,
                'luminance_error': luminance_error,
                'uniformity_error': uniformity_error
            }

        # 打印损失统计
        print("\n校正效果统计：")
        for color in ['red', 'green', 'blue']:
            print(f"{color}通道：")
            print(f"  校正前 - 总损失: {losses['uncorrected'][color]['total_loss']:.2f}, "
                  f"色度误差: {losses['uncorrected'][color]['chromaticity_error']:.2f}, "
                  f"亮度误差: {losses['uncorrected'][color]['luminance_error']:.2f}, "
                  f"均匀性误差: {losses['uncorrected'][color]['uniformity_error']:.2f}")
            print(f"  校正后 - 总损失: {losses['corrected'][color]['total_loss']:.2f}, "
                  f"色度误差: {losses['corrected'][color]['chromaticity_error']:.2f}, "
                  f"亮度误差: {losses['corrected'][color]['luminance_error']:.2f}, "
                  f"均匀性误差: {losses['corrected'][color]['uniformity_error']:.2f}")
            print(f"  改进比例 - 总损失: {(losses['uncorrected'][color]['total_loss'] - losses['corrected'][color]['total_loss']) / losses['uncorrected'][color]['total_loss'] * 100:.2f}%")
        return losses

    def visualize_results(self):
        """可视化校正前后的对比图，并标注损失值。"""
        print("正在生成校正前后对比图...")
        losses = self.calculate_loss()

        plt.rcParams['font.sans-serif'] = ['SimHei']
        plt.rcParams['axes.unicode_minus'] = False

        fig, axes = plt.subplots(2, 3, figsize=(15, 10))
        fig.suptitle('LED显示屏校正前后对比图', fontsize=16, fontweight='bold')

        colors = ['red', 'green', 'blue']
        color_names = ['红色通道', '绿色通道', '蓝色通道']

        for idx, (color, name) in enumerate(zip(colors, color_names)):
            uncorrected_loss = losses['uncorrected'][color]['total_loss']
            axes[0, idx].imshow(self.uncorrected_images[color])
            axes[0, idx].set_title(f'{name}\n校正前 (损失: {uncorrected_loss:.2f})', fontsize=12)
            axes[0, idx].axis('off')

            corrected_loss = losses['corrected'][color]['total_loss']
            axes[1, idx].imshow(self.corrected_images[color])
            axes[1, idx].set_title(f'{name}\n校正后 (损失: {corrected_loss:.2f})', fontsize=12)
            axes[1, idx].axis('off')

        plt.tight_layout(rect=[0, 0, 1, 0.95])
        plt.savefig('led_correction_comparison.png', dpi=300, bbox_inches='tight')
        print("对比图已保存为 'led_correction_comparison.png'")
        plt.show()

# 使用示例
if __name__ == "__main__":
    file_path = "B题附件：RGB数值.xlsx"
    corrector = LEDColorCorrectionSimplified(file_path)
    if corrector.load_data():
        corrector.build_pixel_matrices()
        corrector.calculate_driver_values()
        corrector.simulate_displays()
        corrector.visualize_results()