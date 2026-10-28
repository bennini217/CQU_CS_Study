package com.student.management;

import com.student.management.cli.MenuManager;
import com.student.management.service.DataManager;
import com.student.management.util.ConsoleUtil; // 新增

/**
 * 程序主入口 (V3 - 重构版)
 * * 职责:
 * 1. 欢迎
 * 2. 初始化数据 (调用 DataManager)
 * 3. 启动应用主循环 (调用 MenuManager)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("欢迎来到学生成绩管理系统!");
        System.out.println("系统正在初始化, 请稍候...");

        // ------------------------------------------
        // [阶段一]：生成初始化数据
        ConsoleUtil.simulateLoading(1500, "生成基础数据 (课程, 教师, 班级)..."); // 延时1.5秒
        DataManager.generateInitialData();

        // [阶段二]：模拟学生选课
        ConsoleUtil.simulateLoading(2000, "模拟学生选课并创建成绩记录..."); // 延时2秒
        DataManager.performCourseSelection();

        // [阶段三]：生成学生成绩
        ConsoleUtil.simulateLoading(1000, "随机生成各项成绩并计算综合分..."); // 延时1秒
        DataManager.generateStudentScores();
        // ------------------------------------------

        System.out.println("\n系统初始化完毕! 请登录。"); // 加个换行

        // [阶段四]：启动菜单管理器, 进入主循环
        MenuManager.runApplicationLoop();

        System.out.println("感谢使用，再见!");
    }
}