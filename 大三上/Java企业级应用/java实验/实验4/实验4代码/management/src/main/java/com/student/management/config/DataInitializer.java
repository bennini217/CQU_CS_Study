package com.student.management.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.management.entity.*;
import com.student.management.mapper.*;
import com.student.management.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional; // ❌ 1. 注释掉这个包

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private StudentMapper studentMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private TeachingClassMapper classMapper;
    @Autowired private GradeMapper gradeMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private StudentSemesterGradeMapper semesterGradeMapper;
    @Autowired private PasswordUtil passwordUtil;

    private final Random random = new Random();
    private final String[] DAYS = {"周一", "周二", "周三", "周四", "周五"};
    private final String[] PERIODS = {"1-2节", "3-4节", "5-6节", "7-8节"};

    @Override
    // @Transactional  // ❌ 2. 必须注释掉这一行！！防止回滚！！
    public void run(String... args) {
        try { // ✅ 3. 加上 try-catch 捕获真凶

            // 为了保证每次都生成数据，这里我先帮你把“跳过逻辑”注释掉
            // 如果你想重置数据，请手动去数据库执行 TRUNCATE 命令
            /* if (studentMapper.selectCount(null) > 0) {
                System.out.println(">>> 数据库已有数据，跳过初始化。");
                return;
            }
            */

            System.out.println(">>> 开始强制初始化数据...");

            // 1. 生成课程
            // 加个 try-catch 防止重复主键报错
            try {
                createCourse("C001", "Java程序设计", 4);
                createCourse("C002", "数据结构", 3);
                createCourse("C003", "计算机网络", 3);
            } catch (Exception e) { System.out.println("⚠️ 课程已存在，跳过"); }

            // 2. 生成教师
            String[] teacherNames = {"教师1", "教师2", "教师3", "教师4", "教师5", "教师6"};
            for (int i = 0; i < teacherNames.length; i++) {
                String tid = "T00" + (i + 1);
                try {
                    createTeacher(tid, teacherNames[i]);
                    createSysUser(tid, teacherNames[i], "TEACHER");
                } catch (Exception e) { System.out.println("⚠️ 教师 " + tid + " 已存在，跳过"); }
            }

            // 3. 生成教学班 (🔥🔥 重点关注这里)
            System.out.println(">>> 正在插入教学班...");
            createClass("TC001", "C001", "T001");
            createClass("TC002", "C001", "T002");
            createClass("TC003", "C002", "T003");
            createClass("TC004", "C002", "T004");
            createClass("TC005", "C003", "T005");
            createClass("TC006", "C003", "T006");
            System.out.println("✅ 教学班插入完成！");

            // 4. 生成学生并选课
            String[] firstNames = {"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈"};
            String[] lastNames = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

            for (int i = 1; i <= 100; i++) {
                String sid = String.format("S%03d", i);
                try {
                    // 先检查学生是否已存在
                    Student existing = studentMapper.selectById(sid);
                    if (existing != null) {
                        System.out.println("⚠️ 学生 " + sid + " 已存在，跳过插入");
                        // 即使学生已存在，也尝试创建用户和选课（如果不存在的话）
                        createSysUser(sid, existing.getName(), "STUDENT");
                        continue; // 跳过学生插入，但继续处理选课
                    }
                    
                    String name = firstNames[random.nextInt(firstNames.length)] + lastNames[random.nextInt(lastNames.length)];
                    Student s = new Student(sid, name, random.nextBoolean() ? "男" : "女", "计算机科学与技术", 2023);
                    studentMapper.insert(s);
                    createSysUser(sid, name, "STUDENT");

                    // 选课
                    assignGrade(sid, "C001", "Java程序设计", "TC001", "TC002");
                    assignGrade(sid, "C002", "数据结构", "TC003", "TC004");
                    assignGrade(sid, "C003", "计算机网络", "TC005", "TC006");
                    
                    // 生成往期学期成绩数据
                    createSemesterGrades(sid);
                } catch (Exception e) {
                    // 忽略单个学生的插入错误，继续下一个
                    System.err.println("❌ 学生 " + sid + " 插入失败: " + e.getMessage());
                }
            }

            System.out.println(">>> 🎉🎉🎉 所有数据初始化完成！请去数据库检查！");

        } catch (Exception e) {
            System.err.println("🔥 发生严重错误，但已插入的数据已保存！");
            e.printStackTrace(); // 打印错误堆栈
        }
    }

    // --- 辅助方法 ---

    private void createCourse(String id, String name, int credit) {
        Course existing = courseMapper.selectById(id);
        if (existing == null) {
            courseMapper.insert(new Course(id, name, credit));
        }
    }

    private void createTeacher(String id, String name) {
        Teacher existing = teacherMapper.selectById(id);
        if (existing == null) {
            teacherMapper.insert(new Teacher(id, name));
        }
    }

    // 教学班：带随机时间
    private void createClass(String cid, String courseId, String tid) {
        try {
            TeachingClass existing = classMapper.selectById(cid);
            if (existing != null) {
                return; // 已存在，跳过
            }
            
            TeachingClass tc = new TeachingClass();
            tc.setClassId(cid);
            tc.setCourseId(courseId);
            tc.setTeacherId(tid);
            tc.setSemester("2024-Autumn");
            // 随机生成时间
            tc.setDayOfWeek(DAYS[random.nextInt(DAYS.length)]);
            tc.setTimePeriod(PERIODS[random.nextInt(PERIODS.length)]);
            classMapper.insert(tc);
        } catch (Exception e) {
            System.err.println("❌ 班级 " + cid + " 插入失败: " + e.getMessage());
        }
    }

    private void createSysUser(String username, String name, String type) {
        try {
            User u = new User();
            u.setUsername(username);
            // 使用PasswordUtil加密密码
            // 演示模式：明文存储；生产模式：BCrypt加密存储
            String plainPassword = "123456";
            u.setPassword(passwordUtil.encrypt(plainPassword));
            u.setName(name);
            u.setUserType(type);
            userMapper.insert(u);
        } catch (Exception e) {
            // 用户已存在，忽略
        }
    }

    private void assignGrade(String sid, String courseId, String courseName, String classA, String classB) {
        try {
            String classId = random.nextBoolean() ? classA : classB;
            
            // 检查该学生是否已经选了这门课
            LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Grade::getStudentId, sid)
                   .eq(Grade::getCourseId, courseId);
            Grade existing = gradeMapper.selectOne(wrapper);
            if (existing != null) {
                return; // 已选课，跳过
            }

            Grade g = new Grade();
            g.setStudentId(sid);
            g.setCourseId(courseId);
            g.setCourseName(courseName);
            g.setClassId(classId);
            g.setSemester("2024-Autumn");
            g.setRecordTime(LocalDateTime.now());

            g.setNormalScore(80 + random.nextInt(20));
            g.setMidtermScore(60 + random.nextInt(40));
            g.setLabScore(85 + random.nextInt(15));
            g.setFinalScore(30 + random.nextInt(70));

            double total = g.getNormalScore()*0.1 + g.getMidtermScore()*0.2 + g.getLabScore()*0.2 + g.getFinalScore()*0.5;
            g.setTotalScore(BigDecimal.valueOf(total));

            gradeMapper.insert(g);
        } catch (Exception e) {
            // 忽略选课错误，继续执行
        }
    }

    /**
     * 为学生生成往期学期的平均成绩数据
     * 生成5个学期的数据：大一上、大一下、大二上、大二下、大三上
     * 80%概率基于本学期成绩生成，20%概率随机生成
     */
    private void createSemesterGrades(String studentId) {
        try {
            // 检查是否已存在该学生的学期成绩数据
            LambdaQueryWrapper<StudentSemesterGrade> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudentSemesterGrade::getStudentId, studentId);
            if (semesterGradeMapper.selectCount(wrapper) > 0) {
                return; // 已存在，跳过
            }

            // 定义5个学期的信息（从早到晚）
            String[][] semesters = {
                {"2021-Autumn", "大一上"},
                {"2022-Spring", "大一下"},
                {"2022-Autumn", "大二上"},
                {"2023-Spring", "大二下"},
                {"2023-Autumn", "大三上"}
            };

            // 先获取本学期（2024-Autumn）的平均成绩作为基准
            List<Grade> currentSemesterGrades = gradeMapper.selectList(
                    new LambdaQueryWrapper<Grade>()
                            .eq(Grade::getStudentId, studentId)
                            .eq(Grade::getSemester, "2024-Autumn")); // 当前学期是2024-Autumn
            
            double currentAvgScore = 0;
            if (!currentSemesterGrades.isEmpty()) {
                // 计算本学期平均成绩
                double total = currentSemesterGrades.stream()
                        .mapToDouble(g -> g.getTotalScore().doubleValue())
                        .sum();
                currentAvgScore = total / currentSemesterGrades.size();
            } else {
                // 如果没有本学期成绩，使用默认值
                currentAvgScore = 75.0;
            }

            // 80%概率基于本学期成绩生成，20%概率随机生成
            boolean useCurrentScore = random.nextDouble() < 0.8;
            
            // 生成每个学期的成绩（从早到晚）
            double[] semesterScores = new double[semesters.length];
            
            if (useCurrentScore) {
                // 80%概率：基于本学期成绩，往前推，每个学期递减
                // 本学期（最后一个）使用实际平均成绩
                semesterScores[semesters.length - 1] = currentAvgScore;
                
                // 往前推，每个学期比后一个学期低2-5分
                for (int i = semesters.length - 2; i >= 0; i--) {
                    double nextSemesterScore = semesterScores[i + 1];
                    // 每个学期比后一个学期低2-5分，加上±2的随机波动
                    double decrease = 2 + random.nextInt(4) + (random.nextDouble() * 4 - 2); // 2-5分递减 + ±2波动
                    semesterScores[i] = Math.max(60, Math.min(100, nextSemesterScore - decrease));
                }
            } else {
                // 20%概率：完全随机生成（60-95分之间）
                for (int i = 0; i < semesters.length; i++) {
                    semesterScores[i] = 60 + random.nextDouble() * 35; // 60-95分随机
                }
            }
            
            // 插入数据
            for (int i = 0; i < semesters.length; i++) {
                String semester = semesters[i][0];
                String semesterName = semesters[i][1];
                double avgScore = semesterScores[i];
                
                // 每个学期的课程数量：3-5门
                int courseCount = 3 + random.nextInt(3);
                
                StudentSemesterGrade sg = new StudentSemesterGrade();
                sg.setStudentId(studentId);
                sg.setSemester(semester);
                sg.setSemesterName(semesterName);
                sg.setAvgScore(BigDecimal.valueOf(Math.round(avgScore * 100.0) / 100.0)); // 保留两位小数
                sg.setCourseCount(courseCount);
                sg.setRecordTime(LocalDateTime.now().minusMonths((semesters.length - i) * 6)); // 模拟历史时间
                
                semesterGradeMapper.insert(sg);
            }
        } catch (Exception e) {
            // 忽略错误，继续执行
            System.err.println("❌ 生成学期成绩失败: " + studentId + " - " + e.getMessage());
        }
    }
}