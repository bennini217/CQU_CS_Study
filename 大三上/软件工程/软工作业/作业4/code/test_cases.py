"""
测试用例设计 - 黑盒测试
"""
from datetime import datetime
from copyright_registration_system import CopyrightRegistrationSystem
import os
import json
from PIL import Image, ImageDraw
import shutil
import os


class TestCase:
    """测试用例类"""
    
    def __init__(self, test_id: str, description: str, inputs: dict, expected_output: dict):
        """
        初始化测试用例
        
        Args:
            test_id: 测试用例ID
            description: 测试用例描述
            inputs: 输入数据
            expected_output: 预期输出
        """
        self.test_id = test_id
        self.description = description
        self.inputs = inputs
        self.expected_output = expected_output
        self.actual_output = None
        self.passed = False
    
    def execute(self, system: CopyrightRegistrationSystem) -> dict:
        """
        执行测试用例
        
        Args:
            system: 版权登记系统实例
            
        Returns:
            测试结果
        """
        try:
            success, message, similar_painting = system.register_painting(**self.inputs)
            
            self.actual_output = {
                'success': success,
                'message': message,
                'has_similar_painting': similar_painting is not None
            }
            
            # 判断测试是否通过
            self.passed = (
                self.actual_output['success'] == self.expected_output['success'] and
                self.expected_output['message_keyword'] in self.actual_output['message']
            )
            
            return {
                'test_id': self.test_id,
                'passed': self.passed,
                'expected': self.expected_output,
                'actual': self.actual_output
            }
        except Exception as e:
            self.actual_output = {'error': str(e)}
            return {
                'test_id': self.test_id,
                'passed': False,
                'expected': self.expected_output,
                'actual': self.actual_output,
                'error': str(e)
            }


def create_test_image(width: int, height: int, filename: str, color: tuple = (255, 0, 0), pattern: str = "solid"):
    """
    创建测试图像
    
    Args:
        width: 宽度
        height: 高度
        filename: 文件名
        color: RGB颜色
        pattern: 图案类型 ("solid", "gradient", "checkerboard", "circles")
    """
    img = Image.new('RGB', (width, height), color=color)
    
    # 根据pattern类型添加不同的图案，使每个图像都有独特的特征
    from PIL import ImageDraw
    draw = ImageDraw.Draw(img)
    
    if pattern == "gradient":
        # 渐变效果
        for y in range(height):
            ratio = y / height
            r = int(color[0] * (1 - ratio) + 128 * ratio)
            g = int(color[1] * (1 - ratio) + 128 * ratio)
            b = int(color[2] * (1 - ratio) + 128 * ratio)
            draw.line([(0, y), (width, y)], fill=(r, g, b))
    elif pattern == "checkerboard":
        # 棋盘格
        square_size = 50
        for x in range(0, width, square_size):
            for y in range(0, height, square_size):
                if (x // square_size + y // square_size) % 2 == 0:
                    draw.rectangle([x, y, x + square_size, y + square_size], fill=color)
                else:
                    draw.rectangle([x, y, x + square_size, y + square_size], fill=(255 - color[0], 255 - color[1], 255 - color[2]))
    elif pattern == "circles":
        # 圆形图案
        center_x, center_y = width // 2, height // 2
        radius = min(width, height) // 4
        draw.ellipse([center_x - radius, center_y - radius, center_x + radius, center_y + radius], 
                    fill=(255 - color[0], 255 - color[1], 255 - color[2]))
    # else: solid - 保持纯色，但添加一个小的唯一标识
    else:
        # 在纯色背景上添加一个小点，使每个图像都有微小差异
        draw.ellipse([width - 20, height - 20, width - 10, height - 10], fill=(color[0] ^ 128, color[1] ^ 128, color[2] ^ 128))
    
    img.save(filename)
    return filename


def create_test_images():
    """创建测试所需的图像文件"""
    test_dir = "test_images"
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)
    os.makedirs(test_dir)
    
    # 创建各种测试图像
    images = {}
    
    # 1. 符合分辨率要求的图像 - 使用不同的图案使它们有不同的哈希值
    images['valid_1'] = create_test_image(800, 600, f"{test_dir}/valid_800x600.jpg", (255, 0, 0), "solid")
    images['valid_2'] = create_test_image(1200, 800, f"{test_dir}/valid_1200x800.jpg", (0, 255, 0), "checkerboard")
    images['valid_3'] = create_test_image(1000, 1000, f"{test_dir}/valid_1000x1000.jpg", (0, 0, 255), "circles")
    
    # 2. 分辨率不足的图像
    images['invalid_small'] = create_test_image(600, 400, f"{test_dir}/invalid_600x400.jpg", (255, 255, 0), "circles")
    images['invalid_width'] = create_test_image(700, 800, f"{test_dir}/invalid_700x800.jpg", (255, 0, 255), "gradient")
    images['invalid_height'] = create_test_image(900, 500, f"{test_dir}/invalid_900x500.jpg", (0, 255, 255), "checkerboard")
    
    # 3. 相同的图像（用于测试重复检测）- 使用完全相同的图像
    img1 = Image.new('RGB', (800, 600), color=(128, 128, 128))
    img1.save(f"{test_dir}/duplicate_1.jpg")
    images['duplicate_1'] = f"{test_dir}/duplicate_1.jpg"
    
    # duplicate_2 应该是 duplicate_1 的完全复制
    shutil.copy(f"{test_dir}/duplicate_1.jpg", f"{test_dir}/duplicate_2.jpg")
    images['duplicate_2'] = f"{test_dir}/duplicate_2.jpg"
    
    # 4. 边界值测试 - 使用不同的图案
    images['boundary_exact'] = create_test_image(800, 600, f"{test_dir}/boundary_800x600.jpg", (100, 100, 100), "circles")
    images['boundary_minus_1'] = create_test_image(799, 599, f"{test_dir}/boundary_799x599.jpg", (200, 200, 200), "gradient")
    
    return images


def get_test_cases(test_images: dict) -> list:
    """获取所有测试用例"""
    now = datetime.now()
    
    test_cases = [
        # TC001: 正常登记流程 - 符合所有要求
        TestCase(
            test_id="TC001",
            description="正常登记流程 - 符合分辨率要求，无重复作品",
            inputs={
                'title': '测试作品1',
                'author': '测试作者1',
                'registration_time': now,
                'work_type': '油画',
                'image_path': test_images['valid_1']
            },
            expected_output={
                'success': True,
                'message_keyword': '成功'
            }
        ),
        
        # TC002: 分辨率不足 - 宽度和高度都不足
        TestCase(
            test_id="TC002",
            description="分辨率不足 - 宽度和高度都不足最小要求",
            inputs={
                'title': '测试作品2',
                'author': '测试作者2',
                'registration_time': now,
                'work_type': '水彩画',
                'image_path': test_images['invalid_small']
            },
            expected_output={
                'success': False,
                'message_keyword': '分辨率不足'
            }
        ),
        
        # TC003: 分辨率不足 - 宽度不足
        TestCase(
            test_id="TC003",
            description="分辨率不足 - 宽度小于800",
            inputs={
                'title': '测试作品3',
                'author': '测试作者3',
                'registration_time': now,
                'work_type': '素描',
                'image_path': test_images['invalid_width']
            },
            expected_output={
                'success': False,
                'message_keyword': '分辨率不足'
            }
        ),
        
        # TC004: 分辨率不足 - 高度不足
        TestCase(
            test_id="TC004",
            description="分辨率不足 - 高度小于600",
            inputs={
                'title': '测试作品4',
                'author': '测试作者4',
                'registration_time': now,
                'work_type': '版画',
                'image_path': test_images['invalid_height']
            },
            expected_output={
                'success': False,
                'message_keyword': '分辨率不足'
            }
        ),
        
        # TC005: 边界值测试 - 刚好满足分辨率要求
        TestCase(
            test_id="TC005",
            description="边界值测试 - 分辨率刚好为800x600",
            inputs={
                'title': '测试作品5',
                'author': '测试作者5',
                'registration_time': now,
                'work_type': '国画',
                'image_path': test_images['boundary_exact']
            },
            expected_output={
                'success': True,
                'message_keyword': '成功'
            }
        ),
        
        # TC006: 边界值测试 - 分辨率差1像素
        TestCase(
            test_id="TC006",
            description="边界值测试 - 分辨率为799x599（差1像素）",
            inputs={
                'title': '测试作品6',
                'author': '测试作者6',
                'registration_time': now,
                'work_type': '插画',
                'image_path': test_images['boundary_minus_1']
            },
            expected_output={
                'success': False,
                'message_keyword': '分辨率不足'
            }
        ),
        
        # TC007: 重复作品检测 - 相同图像
        TestCase(
            test_id="TC007",
            description="重复作品检测 - 上传与已存证相同的图像",
            inputs={
                'title': '测试作品7',
                'author': '测试作者7',
                'registration_time': now,
                'work_type': '数字艺术',
                'image_path': test_images['duplicate_1']
            },
            expected_output={
                'success': True,  # 第一次上传应该成功
                'message_keyword': '成功'
            }
        ),
        
        # TC008: 重复作品检测 - 第二次上传相同图像
        TestCase(
            test_id="TC008",
            description="重复作品检测 - 第二次上传相同图像应被拒绝",
            inputs={
                'title': '测试作品8',
                'author': '测试作者8',
                'registration_time': now,
                'work_type': '数字艺术',
                'image_path': test_images['duplicate_2']
            },
            expected_output={
                'success': False,
                'message_keyword': '相似度过高'
            }
        ),
        
        # TC009: 正常登记 - 不同图像
        TestCase(
            test_id="TC009",
            description="正常登记 - 不同颜色和尺寸的图像",
            inputs={
                'title': '测试作品9',
                'author': '测试作者9',
                'registration_time': now,
                'work_type': '抽象画',
                'image_path': test_images['valid_2']
            },
            expected_output={
                'success': True,
                'message_keyword': '成功'
            }
        ),
        
        # TC010: 正常登记 - 另一个不同图像
        TestCase(
            test_id="TC010",
            description="正常登记 - 另一个不同图像",
            inputs={
                'title': '测试作品10',
                'author': '测试作者10',
                'registration_time': now,
                'work_type': '现代艺术',
                'image_path': test_images['valid_3']
            },
            expected_output={
                'success': True,
                'message_keyword': '成功'
            }
        ),
    ]
    
    return test_cases


def run_tests():
    """运行所有测试用例"""
    print("=" * 80)
    print("作品版权登记系统 - 黑盒测试")
    print("=" * 80)
    
    # 创建测试图像
    print("\n正在创建测试图像...")
    test_images = create_test_images()
    print(f"已创建 {len(test_images)} 个测试图像")
    
    # 获取测试用例
    test_cases = get_test_cases(test_images)
    print(f"共 {len(test_cases)} 个测试用例\n")
    
    # 使用独立的测试数据库
    test_db = "test_paintings_db.json"
    if os.path.exists(test_db):
        os.remove(test_db)
    
    system = CopyrightRegistrationSystem(similarity_threshold=10, db_file=test_db)
    
    results = []
    
    # 执行测试用例
    for i, test_case in enumerate(test_cases, 1):
        print(f"执行测试用例 {test_case.test_id}: {test_case.description}")
        result = test_case.execute(system)
        results.append(result)
        
        status = "✓ 通过" if result['passed'] else "✗ 失败"
        print(f"  结果: {status}")
        if not result['passed']:
            print(f"  预期: {result['expected']}")
            print(f"  实际: {result['actual']}")
        print()
    
    # 生成测试报告
    print("=" * 80)
    print("测试报告")
    print("=" * 80)
    
    passed_count = sum(1 for r in results if r['passed'])
    failed_count = len(results) - passed_count
    
    print(f"\n总测试用例数: {len(results)}")
    print(f"通过: {passed_count}")
    print(f"失败: {failed_count}")
    print(f"通过率: {passed_count / len(results) * 100:.1f}%")
    
    print("\n详细测试结果:")
    print("-" * 80)
    for result in results:
        status = "✓" if result['passed'] else "✗"
        print(f"{status} {result['test_id']}: {result['actual']}")
    
    # 保存测试结果到文件
    with open("test_results.json", "w", encoding="utf-8") as f:
        json.dump({
            'summary': {
                'total': len(results),
                'passed': passed_count,
                'failed': failed_count,
                'pass_rate': passed_count / len(results) * 100
            },
            'results': results
        }, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试结果已保存到 test_results.json")
    
    # 清理测试数据库（可选，保留用于查看）
    # if os.path.exists(test_db):
    #     os.remove(test_db)
    
    return results


if __name__ == "__main__":
    run_tests()

