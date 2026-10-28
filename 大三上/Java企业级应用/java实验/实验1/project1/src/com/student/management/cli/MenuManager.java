package com.student.management.cli;

import com.student.management.dto.StudentRank;
import com.student.management.model.*;
import com.student.management.repository.SchoolDatabase;

import java.io.UnsupportedEncodingException; // 确保这个 import 存在
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.student.management.util.ConsoleUtil;

/**
 * 菜单管理器 (UI + Controller)
 * 负责处理所有用户交互 (登录、菜单、功能实现)
 */
public class MenuManager {

    private static Scanner scanner = new Scanner(System.in);
    private static Object currentUser = null;


    public static void runApplicationLoop() {
        System.out.println("系统初始化完毕! 请登录。");
        while (true) {
            boolean loginSuccess = showLoginMenu();
            if (loginSuccess) {
                if (currentUser instanceof Student) {
                    runStudentMenu((Student) currentUser);
                } else if (currentUser instanceof Teacher) {
                    runTeacherMenu((Teacher) currentUser);
                }
            } else {
                System.out.println("登录失败, ID不存在, 请重试。");
            }
            currentUser = null;
            System.out.println("您已退出登录。");
        }
    }

    private static boolean showLoginMenu() {
        System.out.println("\n========== 用户登录 ==========");
        System.out.println("请输入您的ID (例如: 学生 S001, 教师 T001)");
        System.out.print("ID: ");
        String id = scanner.nextLine();
        if (id == null || id.trim().isEmpty()) return false;
        if (id.startsWith("S")) {
            Optional<Student> student = SchoolDatabase.students.stream().filter(s -> s.getStudentId().equals(id)).findFirst();
            if (student.isPresent()) {
                currentUser = student.get();
                System.out.println("登录成功! 欢迎你, " + student.get().getName() + " 同学。");
                return true;
            }
        } else if (id.startsWith("T")) {
            Optional<Teacher> teacher = SchoolDatabase.teachers.stream().filter(t -> t.getTeacherId().equals(id)).findFirst();
            if (teacher.isPresent()) {
                currentUser = teacher.get();
                System.out.println("登录成功! 欢迎您, " + teacher.get().getName() + " 老师。");
                return true;
            }
        }
        return false;
    }

    private static void runStudentMenu(Student student) {
        boolean running = true;
        int menuWidth = 30; // 估计的学生菜单宽度
        while (running) {
            System.out.println(); // 加个空行
            ConsoleUtil.printMenuHeader("学生菜单 ( " + student.getName() + " )", menuWidth);
            System.out.println(" 1. 查询我的所有课程和成绩 ");
            System.out.println(" 2. 查询我的课程与任课教师 " );
            System.out.println(" 0. 退出登录" ); // 手动补空格对齐
            ConsoleUtil.printMenuFooter(menuWidth);
            System.out.print("请输入选择: ");
            int choice = -1;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("输入无效!"); continue;
            }
            switch (choice) {
                case 1: function_Student_QueryMyGrades(student); break;
                case 2: function_Student_QueryMyCourses(student); break;
                case 0: running = false; break;
                default: System.out.println("无效选择!");
            }
        }
    }

    private static void runTeacherMenu(Teacher teacher) {
        boolean running = true;
        int menuWidth = 40; // 估计的教师菜单宽度
        while (running) {
            System.out.println(); // 加个空行
            ConsoleUtil.printMenuHeader("教师菜单 ( " + teacher.getName() + " )", menuWidth);
            System.out.println( " 1. 显示教学班成绩 学号排序             " ); // 手动补空格
            System.out.println(" 2. 显示教学班成绩 成绩排序             " );
            System.out.println(" 3. 统计成绩分数段分布                  " );
            System.out.println(" 4. 查询指定学生所有成绩                 ");
            System.out.println(" 5. 显示所有学生排名             " );
            System.out.println(" 6. 智能预警与建议             "  );
            System.out.println(" 0. 退出登录                   ");
            ConsoleUtil.printMenuFooter(menuWidth);
            System.out.print("请输入选择: ");
            int choice = -1;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("输入无效!"); continue;
            }
            switch (choice) {
                case 1: function_Teacher_ShowClassGrades(teacher, false); break;
                case 2: function_Teacher_ShowClassGrades(teacher, true); break;
                case 3: function_Teacher_ShowGradeDistribution(teacher); break;
                case 4: function_Teacher_QueryStudentGrades(); break;
                case 5: function_Teacher_ShowAllStudentRankings(); break;
                case 6: function_Teacher_AcademicWarning(); break;
                case 0: running = false; break;
                default: System.out.println("无效选择!");
            }
        }
    }
    /**
     * 学生功能1: 查询我的所有课程和成绩
     */
    private static void function_Student_QueryMyGrades(Student student) {
        ConsoleUtil.simulateLoading(1000, "正在为您生成成绩单..."); // 延时2秒
        System.out.println("\n--- " + student.getName() + " (" + student.getStudentId() + ") 的成绩单 ---");

        List<Grade> myGrades = new ArrayList<>();
        for (Grade grade : SchoolDatabase.grades) {
            if (grade.getStudent().equals(student)) {
                myGrades.add(grade);
            }
        }
        if (myGrades.isEmpty()) {
            System.out.println("你还没有任何成绩记录。");
            return;
        }

        double totalScore = 0;
        List<String> warnings = new ArrayList<>();
        List<String> positives = new ArrayList<>();

        // 定义列的视觉宽度
        int w_cId = 12;     // 课程编号
        int w_cName = 18;   // 课程名称
        int w_score1 = 10;  // 平时成绩
        int w_score2 = 10;  // 期中成绩
        int w_score3 = 10;  // 实验成绩
        int w_score4 = 10;  // 期末成绩
        int w_total = 10;   // 综合成绩

        String line = "-".repeat(w_cId + w_cName + w_score1 + w_score2 + w_score3 + w_score4 + w_total);
        System.out.println(line);

        System.out.println(
                padRight("课程编号", w_cId+1)   +
                        padRight("课程名称", w_cName-3) +
                        padRight("平时成绩", w_score1) +
                        padRight("期中成绩", w_score2) +
                        padRight("实验成绩", w_score3) +
                        padRight("期末成绩", w_score4+2) +
                        padRight("综合成绩", w_total)
        );
        System.out.println(line);

        // 3. 遍历打印每一条成绩, 并同时分析
        for (Grade g : myGrades) {
            Course c = g.getTeachingClass().getCourse();

            // 格式化数字为字符串
            String s_normal = String.valueOf(g.getNormalScore());
            String s_midterm = String.valueOf(g.getMidtermScore());
            String s_lab = String.valueOf(g.getLabScore());
            String s_final = String.valueOf(g.getFinalExamScore());
            String s_comp = String.format("%.2f", g.getComprehensiveScore());

            System.out.println(
                    padRight(c.getCourseId(), w_cId)   +
                            padRight(c.getCourseName(), w_cName-1) +
                            padRight(s_normal, w_score1-1) +
                            padRight(s_midterm, w_score2-1) +
                            padRight(s_lab, w_score3-1) +
                            padRight(s_final, w_score4-1) +
                            padRight(s_comp, w_total-1)
            );

            totalScore += g.getComprehensiveScore();
            if (g.getComprehensiveScore() < 60) {
                String suggestion = generateLearningSuggestion(g);
                warnings.add(String.format("【预警】 课程 '%s' (%.2f分) 未及格! %s",
                        c.getCourseName(), g.getComprehensiveScore(), suggestion));
            } else if (g.getComprehensiveScore() >= 90) {
                positives.add(String.format("【优秀】 课程 '%s' (%.2f分) 成绩出色!",
                        c.getCourseName(), g.getComprehensiveScore()));
            }
        }
        System.out.println(line);

        double averageScore = (myGrades.isEmpty()) ? 0 : (totalScore / myGrades.size());
        System.out.printf("总分: %.2f, 平均分: %.2f%n", totalScore, averageScore);

        // 智能建议... (不变)
        System.out.println("\n--- 智能学习建议 ---");
        if (averageScore >= 90) {
            positives.add(String.format("【表扬】 你的平均分 (%.2f) 达到优秀水平, 非常棒！", averageScore));
        } else if (averageScore >= 80) {
            positives.add(String.format("【鼓励】 你的平均分 (%.2f) 良好, 继续努力可以更上一层楼！", averageScore));
        } else if (averageScore > 0 && averageScore < 70) {
            warnings.add(String.format("【注意】 你的平均分 (%.2f) 偏低, 请注意平衡各科学习。", averageScore));
        }
        if (positives.isEmpty() && warnings.isEmpty()) {
            System.out.println("表现平稳, 继续努力。");
        } else {
            for (String p : positives) {
                System.out.println(ConsoleUtil.GREEN + p + ConsoleUtil.RESET);
            }
            // 再打印预警信息 (红色)
            for (String w : warnings) {
                System.out.println(ConsoleUtil.RED + w + ConsoleUtil.RESET);
            }
        }
    }

    /**
     * 学生功能2: 查询我的课程与任课教师
     */
    private static void function_Student_QueryMyCourses(Student student) {
        ConsoleUtil.simulateLoading(1000, "正在为您查询课表..."); // 延时2秒
        System.out.println("\n--- " + student.getName() + " (" + student.getStudentId() + ") 的选课列表 ---");

        List<TeachingClass> myClasses = new ArrayList<>();
        for (TeachingClass tc : SchoolDatabase.teachingClasses) {
            if (tc.getStudents().contains(student)) {
                myClasses.add(tc);
            }
        }
        if (myClasses.isEmpty()) {
            System.out.println("你还没有选择任何课程。");
            return;
        }

        // 定义列宽
        int w_tcId = 14;
        int w_cName = 18;
        int w_tName = 12;
        int w_semester = 22; // "2024-2025秋季学期" 视觉宽度约 19

        String line = "-".repeat(w_tcId + w_cName + w_tName + w_semester);
        System.out.println(line);

        System.out.println(
                padRight("教学班号", w_tcId+1) +
                        padRight("课程名称", w_cName-2) +
                        padRight("任课教师", w_tName+2) +
                        padRight("开课学期", w_semester)
        );
        System.out.println(line);

        for (TeachingClass tc : myClasses) {
            System.out.println(
                    padRight(tc.getClassId(), w_tcId) +
                            padRight(tc.getCourse().getCourseName(), w_cName) +
                            padRight(tc.getTeacher().getName(), w_tName) +
                            padRight(tc.getSemester(), w_semester)
            );
        }
        System.out.println(line);
    }


    private static TeachingClass selectTeachingClass(Teacher teacher) {
        ConsoleUtil.simulateLoading(500, "正在查询您的课程..."); // 延时2秒
        List<TeachingClass> taughtClasses = new ArrayList<>();
        for (TeachingClass tc : SchoolDatabase.teachingClasses) {
            if (tc.getTeacher().equals(teacher)) {
                taughtClasses.add(tc);
            }
        }
        if (taughtClasses.isEmpty()) {
            System.out.println(teacher.getName() + " 老师, 您目前没有教授任何班级。");
            return null;
        }
        System.out.println("您教授的班级如下:");
        for (int i = 0; i < taughtClasses.size(); i++) {
            TeachingClass tc = taughtClasses.get(i);
            System.out.println((i + 1) + ". " + tc.getClassId() + " (" + tc.getCourse().getCourseName() + ")");
        }
        System.out.println("0. 返回上一层");
        System.out.print("请选择一个班级 (输入序号): ");
        int choice = -1;
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("输入无效!");
            return null;
        }
        if (choice > 0 && choice <= taughtClasses.size()) {
            return taughtClasses.get(choice - 1);
        }
        return null;
    }


    /**
     * 教师功能 1 /2: 显示教学班学生成绩
     */
    private static void function_Teacher_ShowClassGrades(Teacher teacher, boolean sortByScore) {
        TeachingClass selectedClass = selectTeachingClass(teacher);
        if (selectedClass == null) {
            System.out.println("已取消操作。");
            return;
        }
        List<Grade> classGrades = new ArrayList<>();
        for (Grade grade : SchoolDatabase.grades) {
            if (grade.getTeachingClass().equals(selectedClass)) {
                classGrades.add(grade);
            }
        }
        if (classGrades.isEmpty()) {
            System.out.println("该班级 (" + selectedClass.getClassId() + ") 暂无学生成绩。");
            return;
        }
        if (sortByScore) {
            classGrades.sort(Comparator.comparingDouble(Grade::getComprehensiveScore).reversed());
        } else {
            classGrades.sort(Comparator.comparing(g -> g.getStudent().getStudentId()));
        }

        ConsoleUtil.simulateLoading(2000, "正在生成成绩单..."); // 延时2秒
        System.out.println("\n--- 班级 " + selectedClass.getClassId() + " 成绩单 ---");
        System.out.println("课程: " + selectedClass.getCourse().getCourseName());
        System.out.println("教师: " + selectedClass.getTeacher().getName());
        System.out.println("总人数: " + selectedClass.getTotalStudents() + " 人");
        System.out.println(sortByScore ? "(按综合成绩 降序 排列)" : "(按学号 升序 排列)");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 定义列的视觉宽度
        int w_rank = 6;
        int w_sId = 12;
        int w_sName = 12;
        int w_score1 = 10;
        int w_score2 = 10;
        int w_score3 = 10;
        int w_score4 = 10;
        int w_total = 10;
        int w_time = 18;

        String line = "-".repeat(w_rank + w_sId + w_sName + w_score1 + w_score2 + w_score3 + w_score4 + w_total + w_time);
        System.out.println(line);

        System.out.println(
                padRight("排名", w_rank)     +
                        padRight("学号", w_sId)     +
                        padRight("姓名", w_sName-2)     +
                        padRight("平时成绩", w_score1) +  // [修正] 确保右对齐
                        padRight("期中成绩", w_score2) +  // [修正] 确保右对齐
                        padRight("实验成绩", w_score3) +  // [修正] 确保右对齐
                        padRight("期末成绩", w_score4) +  // [修正] 确保右对齐
                        padRight("综合成绩", w_total)   +  // [修正] 确保右对齐
                        padRight("记录时间", w_time)
        );
        System.out.println(line);

        int rank = 1;
        for (Grade g : classGrades) {
            Student s = g.getStudent();
            String s_rank = String.valueOf(rank++);
            String s_sId = s.getStudentId();
            String s_sName = s.getName();
            String s_normal = String.valueOf(g.getNormalScore());
            String s_midterm = String.valueOf(g.getMidtermScore());
            String s_lab = String.valueOf(g.getLabScore());
            String s_final = String.valueOf(g.getFinalExamScore());
            String s_comp = String.format("%.2f", g.getComprehensiveScore());
            String s_time = g.getRecordTime().format(formatter);

            System.out.println(
                    padRight(s_rank, w_rank)     +
                            padRight(s_sId, w_sId-1)     +
                            padRight(s_sName, w_sName-1)     +
                            padRight(s_normal, w_score1-1) +
                            padRight(s_midterm, w_score2-1) +
                            padRight(s_lab, w_score3-1) +
                            padRight(s_final, w_score4-1) +
                            padRight(s_comp, w_total-3)   +
                            padRight(s_time, w_time-1)
            );
        }
        System.out.println(line);
    }


    private static void function_Teacher_ShowGradeDistribution(Teacher teacher) {
        System.out.println("\n--- 统计成绩分数段分布 ---");
        System.out.println("1. 统计 [教学班综合成绩] 分布 (您教的班级)");
        System.out.println("2. 统计 [全校学生总成绩] 分布");
        System.out.println("0. 返回");
        System.out.print("请输入选择: ");
        int choice = -1;
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("输入无效!"); return;
        }
        switch (choice) {
            case 1:
                TeachingClass selectedClass = selectTeachingClass(teacher);
                if (selectedClass != null) {
                    calculateAndDisplayDistribution(selectedClass);
                }
                break;
            case 2:
                calculateAndDisplayAllStudentTotalScoreDistribution();
                break;
            case 0: return;
            default: System.out.println("无效选择!");
        }
    }

    private static void calculateAndDisplayDistribution(TeachingClass tc) {
        List<Double> scores = new ArrayList<>();
        for (Grade grade : SchoolDatabase.grades) {
            if (grade.getTeachingClass().equals(tc)) {
                scores.add(grade.getComprehensiveScore());
            }
        }
        if (scores.isEmpty()) {
            System.out.println("班级 " + tc.getClassId() + " 暂无成绩数据。");
            return;
        }
        int[] bins = new int[5];
        for (double score : scores) {
            if (score < 60) bins[0]++;
            else if (score < 70) bins[1]++;
            else if (score < 80) bins[2]++;
            else if (score < 90) bins[3]++;
            else bins[4]++;
        }
        ConsoleUtil.simulateLoading(1000, "正在生成成绩分数段分布..."); // 延时2秒
        System.out.println("\n--- 班级 [" + tc.getClassId() + "] 成绩分布 (" + scores.size() + "人) ---");
        System.out.println("课程: " + tc.getCourse().getCourseName());
        System.out.println("----------------------------------------");
        System.out.printf("%-10s | %-5s | %-5s%n", "分数段", "人数", "比例");
        System.out.println("----------------------------------------");
        System.out.printf("%-12s | %-6d | %-5.1f%%%n", "90 - 100", bins[4], (double)bins[4] / scores.size() * 100);
        System.out.printf("%-12s | %-6d | %-5.1f%%%n", "80 - 89", bins[3], (double)bins[3] / scores.size() * 100);
        System.out.printf("%-12s | %-6d | %-5.1f%%%n", "70 - 79", bins[2], (double)bins[2] / scores.size() * 100);
        System.out.printf("%-12s | %-6d | %-5.1f%%%n", "60 - 69", bins[1], (double)bins[1] / scores.size() * 100);
        System.out.printf("%-10s | %-6d | %-5.1f%%%n", "< 60 (不及格)", bins[0], (double)bins[0] / scores.size() * 100);
        System.out.println("----------------------------------------");
    }

    private static void calculateAndDisplayAllStudentTotalScoreDistribution() {
        List<Double> totalScores = new ArrayList<>();
        for (Student student : SchoolDatabase.students) {
            double totalScore = 0;
            for (Grade grade : SchoolDatabase.grades) {
                if (grade.getStudent().equals(student)) {
                    totalScore += grade.getComprehensiveScore();
                }
            }
            totalScores.add(totalScore);
        }
        int[] bins = new int[4];
        for (double score : totalScores) {
            if (score < 210) bins[0]++;
            else if (score < 240) bins[1]++;
            else if (score < 270) bins[2]++;
            else bins[3]++;
        }
        ConsoleUtil.simulateLoading(1000, "正在生成成绩分数段分布..."); // 延时2秒
        System.out.println("\n--- [全校学生总成绩] 分布 (" + totalScores.size() + "人, 3门课总分) ---");
        System.out.println("----------------------------------------");
        System.out.printf("%-15s | %-5s | %-5s%n", "总分分数段", "人数", "比例");
        System.out.println("----------------------------------------");
        System.out.printf("%-15s | %-5d | %-5.1f%%%n", "270 - 300 (优秀)", bins[3], (double)bins[3] / totalScores.size() * 100);
        System.out.printf("%-15s | %-5d | %-5.1f%%%n", "240 - 269 (良好)", bins[2], (double)bins[2] / totalScores.size() * 100);
        System.out.printf("%-15s | %-5d | %-5.1f%%%n", "210 - 239 (中等)", bins[1], (double)bins[1] / totalScores.size() * 100);
        System.out.printf("%-15s | %-5d | %-5.1f%%%n", "< 210 (一般)", bins[0], (double)bins[0] / totalScores.size() * 100);
        System.out.println("----------------------------------------");
    }

    private static void function_Teacher_QueryStudentGrades() {
        System.out.println("\n--- 查询学生成绩 ---");
        System.out.print("请输入要查询的学生 [学号] 或 [姓名]: ");
        String input = scanner.nextLine();
        if (input == null || input.trim().isEmpty()) {
            System.out.println("输入为空, 已取消。"); return;
        }
        List<Student> foundStudents = new ArrayList<>();
        if (input.toUpperCase().startsWith("S")) {
            for (Student s : SchoolDatabase.students) {
                if (s.getStudentId().equalsIgnoreCase(input)) {
                    foundStudents.add(s); break;
                }
            }
        }
        if (foundStudents.isEmpty()) {
            for (Student s : SchoolDatabase.students) {
                if (s.getName().contains(input)) {
                    foundStudents.add(s);
                }
            }
        }
        if (foundStudents.isEmpty()) {
            System.out.println("未找到学号或姓名包含 '" + input + "' 的学生。");
            return;
        }
        Student studentToQuery = null;
        if (foundStudents.size() == 1) {
            studentToQuery = foundStudents.get(0);
        } else {
            System.out.println("找到了多个同名学生, 请选择:");
            for (int i = 0; i < foundStudents.size(); i++) {
                Student s = foundStudents.get(i);
                System.out.println((i + 1) + ". " + s.getStudentId() + " - " + s.getName());
            }
            System.out.println("0. 取消");
            System.out.print("请输入序号: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice > 0 && choice <= foundStudents.size()) {
                    studentToQuery = foundStudents.get(choice - 1);
                } else {
                    System.out.println("已取消。"); return;
                }
            } catch (NumberFormatException e) {
                System.out.println("输入无效, 已取消。"); return;
            }
        }
        if (studentToQuery != null) {
            function_Student_QueryMyGrades(studentToQuery);
        }
    }


    /**
     * 教师功能 5: 显示所有学生排名
     */
    private static void function_Teacher_ShowAllStudentRankings() {
        ConsoleUtil.simulateLoading(2000, "正在生成全校学生总成绩排名..."); // 延时2秒
        System.out.println("\n--- 全校学生总成绩排名 (共 " + SchoolDatabase.students.size() + " 人) ---");

        List<StudentRank> studentRanks = new ArrayList<>();
        for (Student student : SchoolDatabase.students) {
            StudentRank rankData = new StudentRank(student);
            for (Grade grade : SchoolDatabase.grades) {
                if (grade.getStudent().equals(student)) {
                    rankData.addScore(grade.getComprehensiveScore());
                }
            }
            if (rankData.getCourseCount() > 0) {
                studentRanks.add(rankData);
            }
        }
        studentRanks.sort(Comparator.comparingDouble(StudentRank::getTotalScore).reversed());

        // 定义列的视觉宽度
        int w_rank = 10;
        int w_sId = 12;
        int w_sName = 12;
        int w_total = 14;
        int w_avg = 12;

        String line = "-".repeat(w_rank + w_sId + w_sName + w_total + w_avg);
        System.out.println(line);

        System.out.println(
                padRight("排名", w_rank)      +
                        padRight("学号", w_sId)      +
                        padRight("姓名", w_sName+3)      +
                        padLeft("总成绩(3门)", w_total) + // [修正] 确保右对齐
                        padLeft("平均成绩", w_avg)       // [修正] 确保右对齐
        );
        System.out.println(line);

        int rank = 1;
        for (StudentRank sr : studentRanks) {
            Student s = sr.getStudent();
            String s_rank = String.valueOf(rank++);
            String s_sId = s.getStudentId();
            String s_sName = s.getName();
            String s_total = String.format("%.2f", sr.getTotalScore());
            String s_avg = String.format("%.2f", sr.getAverageScore());

            System.out.println(
                    padRight(s_rank, w_rank)   +
                            padRight(s_sId, w_sId)   +
                            padRight(s_sName, w_sName)   +
                            padLeft(s_total, w_total) +
                            padLeft(s_avg, w_avg)
            );
        }
        System.out.println(line);
    }


    private static void function_Teacher_AcademicWarning() {
        System.out.println("\n--- 智能预警和学习建议 ---");
        Set<Student> warnedStudents = new HashSet<>();
        int warningCount = 0;
        System.out.println("1. 检查 [您教的班级] 的学生");
        System.out.println("2. 检查 [全校所有] 学生");
        System.out.println("0. 返回");
        System.out.print("请输入选择: ");
        int choice = -1;
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("输入无效!"); return;
        }
        List<Grade> gradesToScan = new ArrayList<>();
        String scopeName = "";
        switch (choice) {
            case 1:
                Teacher teacher = (Teacher) currentUser;
                for (TeachingClass tc : SchoolDatabase.teachingClasses) {
                    if (tc.getTeacher().equals(teacher)) {
                        for (Grade g : SchoolDatabase.grades) {
                            if (g.getTeachingClass().equals(tc)) {
                                gradesToScan.add(g);
                            }
                        }
                    }
                }
                scopeName = "您所教的班级中";
                break;
            case 2:
                gradesToScan.addAll(SchoolDatabase.grades);
                scopeName = "全校";
                break;
            case 0: return;
            default: System.out.println("无效选择!"); return;
        }
        if (gradesToScan.isEmpty()) {
            System.out.println(scopeName + " 没有可分析的成绩记录。");
            return;
        }
        System.out.println("\n--- " + scopeName + " 的学业预警报告 ---");
        for (Grade grade : gradesToScan) {
            if (grade.getComprehensiveScore() < 60) {
                warningCount++;
                Student student = grade.getStudent();
                warnedStudents.add(student);
                String courseName = grade.getTeachingClass().getCourse().getCourseName();
                String suggestion = generateLearningSuggestion(grade);
                System.out.println(ConsoleUtil.RED + "----------------------------------------" + ConsoleUtil.RESET);
                System.out.printf(ConsoleUtil.RED + "[预警] 学生: %s (%s)\n" + ConsoleUtil.RESET, student.getName(), student.getStudentId());
                System.out.printf(ConsoleUtil.RED + "     课程: %s (综合成绩: %.2f)\n" + ConsoleUtil.RESET, courseName, grade.getComprehensiveScore());
                System.out.printf(ConsoleUtil.RED + "     %s\n" + ConsoleUtil.RESET, suggestion);
                // --- 修改结束 ---
            }
        }
        if (warningCount == 0) {
            System.out.println("分析完毕: " + scopeName + " 没有发现不及格的学生, 表现良好!");
        } else {
            System.out.println("----------------------------------------");
            System.out.println("分析完毕: 共发现 " + warningCount + " 条不及格记录, 涉及 " + warnedStudents.size() + " 名学生。");
        }
    }

    private static String generateLearningSuggestion(Grade grade) {
        int lowThreshold = 70;
        List<String> lowAreas = new ArrayList<>();
        if (grade.getNormalScore() < lowThreshold) lowAreas.add("平时表现");
        if (grade.getMidtermScore() < lowThreshold) lowAreas.add("期中考试");
        if (grade.getLabScore() < lowThreshold) lowAreas.add("实验");
        if (grade.getFinalExamScore() < lowThreshold) lowAreas.add("期末考试");
        if (lowAreas.isEmpty()) {
            if (grade.getComprehensiveScore() < 60) {
                return "建议: 成绩各项比较均衡, 但需整体提高。";
            }
            if (grade.getComprehensiveScore() >= 90) {
                return "表现出色, 各项均衡, 继续保持！";
            }
            return "表现良好, 各项均衡。";
        } else {
            return "建议: 请重点加强 " + String.join(", ", lowAreas) + " 部分的投入。";
        }
    }


    private static int getVisualWidth(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        try {
            // GBK 编码中, 汉字占2字节, 英文占1字节
            return s.getBytes("GBK").length;
        } catch (UnsupportedEncodingException e) {
            // 如果不支持GBK (不太可能), 回退到一个估算
            return s.length();
        }
    }

    private static String padRight(String s, int targetVisualWidth) {
        int currentWidth = getVisualWidth(s);
        int paddingNeeded = targetVisualWidth - currentWidth;
        if (paddingNeeded <= 0) {
            return s;
        }

        // 使用 StringBuilder 来构建填充
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < paddingNeeded; i++) {
            sb.append(' '); // 填充空格 (宽度为1)
        }
        return sb.toString();
    }

    private static String padLeft(String s, int targetVisualWidth) {
        int currentWidth = getVisualWidth(s);
        int paddingNeeded = targetVisualWidth - currentWidth;
        if (paddingNeeded <= 0) {
            return s;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paddingNeeded; i++) {
            sb.append(' '); // 填充空格
        }
        sb.append(s); // 将字符串加在后面
        return sb.toString();
    }

    private static String d(String s, int targetVisualWidth) {
        int currentWidth = getVisualWidth(s);
        int paddingNeeded = targetVisualWidth - currentWidth;
        if (paddingNeeded <= 0) {
            return s;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paddingNeeded; i++) {
            sb.append(' '); // 填充空格
        }
        sb.append(s); // 将字符串加在后面
        return sb.toString();
    }
}