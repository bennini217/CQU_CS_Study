"""
主程序入口 - 提供命令行界面
"""
import sys
import os
from datetime import datetime
from copyright_registration_system import CopyrightRegistrationSystem


def print_separator():
    """打印分隔线"""
    print("=" * 60)


def main():
    """主函数"""
    print_separator()
    print("欢迎使用作品版权登记系统")
    print_separator()
    
    system = CopyrightRegistrationSystem(similarity_threshold=10)
    
    while True:
        print("\n请选择操作：")
        print("1. 登记绘画版权")
        print("2. 查看所有已登记作品")
        print("3. 退出系统")
        
        choice = input("\n请输入选项（1-3）: ").strip()
        
        if choice == '1':
            register_painting(system)
        elif choice == '2':
            view_all_paintings(system)
        elif choice == '3':
            print("\n感谢使用，再见！")
            break
        else:
            print("无效的选项，请重新选择！")


def register_painting(system: CopyrightRegistrationSystem):
    """登记绘画版权"""
    print_separator()
    print("登记绘画版权")
    print_separator()
    
    # 获取基本信息
    title = input("请输入作品标题: ").strip()
    if not title:
        print("标题不能为空！")
        return
    
    author = input("请输入作者: ").strip()
    if not author:
        print("作者不能为空！")
        return
    
    work_type = input("请输入作品类型: ").strip()
    if not work_type:
        print("作品类型不能为空！")
        return
    
    image_path = input("请输入图像文件路径: ").strip()
    if not image_path:
        print("图像路径不能为空！")
        return
    
    if not os.path.exists(image_path):
        print(f"图像文件不存在: {image_path}")
        return
    
    registration_time = datetime.now()
    
    # 执行登记
    print("\n正在处理...")
    success, message, similar_painting = system.register_painting(
        title=title,
        author=author,
        registration_time=registration_time,
        work_type=work_type,
        image_path=image_path
    )
    
    print_separator()
    if success:
        print(f"✓ {message}")
    else:
        print(f"✗ {message}")
        if similar_painting:
            print("\n最相似的已存证作品信息：")
            print(f"  ID: {similar_painting.painting_id}")
            print(f"  标题: {similar_painting.title}")
            print(f"  作者: {similar_painting.author}")
            print(f"  作品类型: {similar_painting.work_type}")
            print(f"  登记时间: {similar_painting.registration_time}")
            print(f"  图像路径: {similar_painting.image_path}")
    print_separator()


def view_all_paintings(system: CopyrightRegistrationSystem):
    """查看所有已登记作品"""
    print_separator()
    print("所有已登记作品")
    print_separator()
    
    paintings = system.get_all_paintings()
    
    if not paintings:
        print("暂无已登记作品")
    else:
        print(f"共 {len(paintings)} 件作品：\n")
        for painting in paintings:
            print(f"ID: {painting.painting_id}")
            print(f"  标题: {painting.title}")
            print(f"  作者: {painting.author}")
            print(f"  作品类型: {painting.work_type}")
            print(f"  登记时间: {painting.registration_time}")
            print(f"  分辨率: {painting.width} x {painting.height}")
            print(f"  图像路径: {painting.image_path}")
            print()
    
    print_separator()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n程序被用户中断")
        sys.exit(0)
    except Exception as e:
        print(f"\n发生错误: {e}")
        sys.exit(1)



