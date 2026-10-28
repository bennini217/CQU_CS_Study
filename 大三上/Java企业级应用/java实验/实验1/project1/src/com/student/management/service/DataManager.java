package com.student.management.service;

import com.student.management.model.*;
import com.student.management.repository.SchoolDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数据管理器
 * 负责处理所有的数据逻辑：
 * 1. 初始化（生成）数据
 * 2. 学生选课
 * 3. 成绩生成与计算
 */
public class DataManager {

    // 创建一个随机数生成器，我们后面会经常用到
    private static final Random random = new Random();

    /**
     * 生成初始化数据
     */
    public static void generateInitialData() {
        System.out.println("开始生成初始化数据...");

        // 1. 生成课程 (不少于3门)
        generateCourses();
        // 2. 生成教师 (不少于6个)
        generateTeachers();
        // 3. 生成教学班 (满足 "一门课至少有两个老师上课")
        generateTeachingClasses();
        // 4. 生成学生 (不少于100人)
        generateStudents();

        System.out.println("--- 初始化数据生成完毕 ---");
        System.out.println("总共生成课程: " + SchoolDatabase.courses.size() + " 门");
        System.out.println("总共生成教师: " + SchoolDatabase.teachers.size() + " 名");
        System.out.println("总共生成教学班: " + SchoolDatabase.teachingClasses.size() + " 个");
        System.out.println("总共生成学生: " + SchoolDatabase.students.size() + " 名");
        System.out.println("---------------------------------");
    }

    /**
     * 1. 生成课程
     */
    private static void generateCourses() {
        Course c1 = new Course("C001", "Java程序设计");
        Course c2 = new Course("C002", "数据结构");
        Course c3 = new Course("C003", "计算机网络");

        SchoolDatabase.courses.add(c1);
        SchoolDatabase.courses.add(c2);
        SchoolDatabase.courses.add(c3);
    }

    /**
     * 2. 生成教师
     */
    private static void generateTeachers() {
        SchoolDatabase.teachers.add(new Teacher("T001", "段雯菀"));
        SchoolDatabase.teachers.add(new Teacher("T002", "胡兰尹"));
        SchoolDatabase.teachers.add(new Teacher("T003", "郭雪颜"));
        SchoolDatabase.teachers.add(new Teacher("T004", "康来"));
        SchoolDatabase.teachers.add(new Teacher("T005", "唐谢佳"));
        SchoolDatabase.teachers.add(new Teacher("T006", "吴昱颖"));
    }

    /**
     * 3. 生成教学班
     * 需求: "一门课至少有两个老师上课"
     * 实现: 我们有3门课，6个老师。我们给每门课分配2个老师，创建 3 * 2 = 6 个教学班
     */
    private static void generateTeachingClasses() {
        String semester = "2024-2025秋季学期"; // 假设都在一个学期

        // 课程1: Java
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC001", semester, SchoolDatabase.teachers.get(0), SchoolDatabase.courses.get(0)
        ));
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC002", semester, SchoolDatabase.teachers.get(1), SchoolDatabase.courses.get(0)
        ));

        // 课程2: 数据结构
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC003", semester, SchoolDatabase.teachers.get(2), SchoolDatabase.courses.get(1)
        ));
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC004", semester, SchoolDatabase.teachers.get(3), SchoolDatabase.courses.get(1)
        ));

        // 课程3: 计算机网络
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC005", semester, SchoolDatabase.teachers.get(4), SchoolDatabase.courses.get(2)
        ));
        SchoolDatabase.teachingClasses.add(new TeachingClass(
                "TC006", semester, SchoolDatabase.teachers.get(5), SchoolDatabase.courses.get(2)
        ));
    }

    /**
     * 4. 生成学生
     * 需求: "数量不少于100"
     */
    private static void generateStudents() {
        String[] firstNames = {"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈"};
        String[] lastNames = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

        for (int i = 1; i <= 100; i++) {
            // 格式化学号, e.g., "S001", "S010", "S100"
            String studentId = String.format("S%03d", i);

            // 随机姓名
            String name = firstNames[random.nextInt(firstNames.length)] +
                    lastNames[random.nextInt(lastNames.length)];

            // 随机性别
            String gender = random.nextBoolean() ? "男" : "女";

            SchoolDatabase.students.add(new Student(studentId, name, gender));
        }
    }


    /**
     * 阶段二：学生选课
     */
    public static void performCourseSelection() {
        System.out.println("开始处理学生选课...");

        // 我们的策略: 所有学生都选修所有3门课程

        // 1. 遍历所有学生
        for (Student student : SchoolDatabase.students) {

            // 2. 遍历所有课程
            for (Course course : SchoolDatabase.courses) {

                // 3. 为该课程找到所有开设的教学班
                List<TeachingClass> availableClasses = findClassesForCourse(course);

                if (availableClasses.isEmpty()) {
                    System.out.println("警告: 课程 '" + course.getCourseName() + "' 没有开设教学班!");
                    continue; // 跳过这门课
                }

                // 4. 随机选择一个教学班
                // 策略：随机分配，以使班级人数大致均衡
                TeachingClass chosenClass = availableClasses.get(random.nextInt(availableClasses.size()));

                // 5. 执行选课
                // a. 将学生添加到教学班的学生列表中
                chosenClass.addStudent(student);

                // b. 创建一个成绩(Grade)对象, 关联学生和教学班
                //    这个Grade对象现在是空的，等待后续填入成绩
                Grade grade = new Grade(student, chosenClass);

                // c. 将成绩对象存入数据库
                SchoolDatabase.grades.add(grade);
            }
        }

        System.out.println("--- 学生选课处理完毕 ---");
        System.out.println("总共生成 " + SchoolDatabase.grades.size() + " 条选课/成绩记录。");

        // 验证教学班人数是否满足要求 (不少于20人)
        System.out.println("各教学班人数统计：");
        for (TeachingClass tc : SchoolDatabase.teachingClasses) {
            System.out.println(
                    tc.getClassId() + " (" + tc.getCourse().getCourseName() + ", " +
                            tc.getTeacher().getName() + "): " + tc.getTotalStudents() + " 人"
            );
        }
        System.out.println("---------------------------------");
    }

    /**
     * 辅助方法：根据课程(Course)查找所有对应的教学班(TeachingClass)
     */
    private static List<TeachingClass> findClassesForCourse(Course course) {
        // 创建一个空列表来存放结果
        List<TeachingClass> result = new ArrayList<>();

        // 遍历数据库中所有的教学班
        for (TeachingClass tc : SchoolDatabase.teachingClasses) {
            // 如果这个教学班的课程 与 我们要找的课程 相同
            if (tc.getCourse().equals(course)) {
                result.add(tc); // 添加到结果列表
            }
        }
        return result; // 返回所有匹配的教学班
    }

    // 粘贴到 DataManager.java 文件的末尾

    /**
     * 阶段三：生成与计算成绩
     * 自定义各项成绩的产生策略
     */
    public static void generateStudentScores() {
        System.out.println("开始生成所有学生的各项成绩...");

        if (SchoolDatabase.grades.isEmpty()) {
            System.out.println("错误: 没有任何选课记录, 无法生成成绩!");
            return;
        }

        // 遍历所有 300 条 Grade 记录

        // 1. 随机生成4项成绩 (均为60到100之间的整数)
        // random.nextInt(41) 会产生 0-40 之间的整数, +60 即为 60-100
        for (Grade grade : SchoolDatabase.grades) {

            int normalScore = random.nextInt(21) + 80;
            int midtermScore = random.nextInt(61) + 40;
            int labScore = random.nextInt(51) + 50;
            int finalExamScore = random.nextInt(61) + 40;

            // 2. 将成绩设置到 Grade 对象中
            grade.setNormalScore(normalScore);
            grade.setMidtermScore(midtermScore);
            grade.setLabScore(labScore);
            grade.setFinalExamScore(finalExamScore);

            // 3. 调用方法计算综合成绩 (并记录时间)
            grade.calculateComprehensiveScore();
        }

        System.out.println("--- 所有 " + SchoolDatabase.grades.size() + " 条成绩记录生成并计算完毕 ---");
        System.out.println("---------------------------------");
    }



}