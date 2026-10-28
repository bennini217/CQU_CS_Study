-- ============================================
-- 学生成绩管理系统 - 数据库初始化脚本
-- ============================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS student_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE student_db;

-- ============================================
-- 2. 删除所有旧表（按依赖顺序删除，先删除有外键的表）
-- ============================================
SET FOREIGN_KEY_CHECKS = 0;  -- 临时禁用外键检查

DROP TABLE IF EXISTS student_semester_grade;
DROP TABLE IF EXISTS grade;
DROP TABLE IF EXISTS teaching_class;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;  -- 重新启用外键检查

-- ============================================
-- 3. 创建数据表
-- ============================================

-- 3.1 用户表（sys_user）
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（S001/T001格式）',
    password VARCHAR(255) NOT NULL COMMENT '密码（演示模式为明文，生产模式为BCrypt加密）',
    user_type VARCHAR(20) NOT NULL COMMENT '用户类型：STUDENT或TEACHER',
    name VARCHAR(100) COMMENT '用户姓名',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 3.2 学生表（student）
CREATE TABLE student (
    student_id VARCHAR(20) PRIMARY KEY COMMENT '学生ID（S001格式）',
    name VARCHAR(100) NOT NULL COMMENT '学生姓名',
    gender VARCHAR(10) COMMENT '性别',
    major VARCHAR(100) COMMENT '专业',
    grade_year INT COMMENT '年级'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 3.3 教师表（teacher）
CREATE TABLE teacher (
    teacher_id VARCHAR(20) PRIMARY KEY COMMENT '教师ID（T001格式）',
    name VARCHAR(100) NOT NULL COMMENT '教师姓名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- 3.4 课程表（course）
CREATE TABLE course (
    course_id VARCHAR(20) PRIMARY KEY COMMENT '课程ID（C001格式）',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    credit INT COMMENT '学分'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 3.5 教学班表（teaching_class）
CREATE TABLE teaching_class (
    class_id VARCHAR(20) PRIMARY KEY COMMENT '教学班ID（TC001格式）',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    teacher_id VARCHAR(20) NOT NULL COMMENT '教师ID',
    semester VARCHAR(50) COMMENT '学期（2024-Autumn格式）',
    day_of_week VARCHAR(20) COMMENT '上课日期（周一、周二等）',
    time_period VARCHAR(20) COMMENT '上课时间（1-2节、3-4节等）',
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班表';

-- 3.6 成绩表（grade）
CREATE TABLE grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学生ID',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    class_id VARCHAR(20) COMMENT '教学班ID',
    course_name VARCHAR(100) COMMENT '课程名称（冗余字段，方便查询）',
    normal_score INT COMMENT '平时成绩',
    midterm_score INT COMMENT '期中成绩',
    lab_score INT COMMENT '实验成绩',
    final_score INT COMMENT '期末成绩',
    total_score DECIMAL(5,2) COMMENT '综合成绩',
    semester VARCHAR(50) COMMENT '学期',
    record_time DATETIME COMMENT '记录时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- 3.7 学生学期成绩表（student_semester_grade）
CREATE TABLE student_semester_grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学生ID',
    semester VARCHAR(50) NOT NULL COMMENT '学期（2021-Autumn格式）',
    semester_name VARCHAR(50) COMMENT '学期名称（大一上、大一下等）',
    avg_score DECIMAL(5,2) COMMENT '该学期平均成绩',
    course_count INT COMMENT '该学期课程数量',
    record_time DATETIME COMMENT '记录时间',
    INDEX idx_student_id (student_id),
    INDEX idx_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学期成绩表（用于成绩走势分析）';

-- ============================================
-- 4. 数据说明
-- ============================================
-- 
-- 注意：本SQL文件只负责创建数据库表结构
-- 测试数据由 grade-service 的 DataInitializer 自动生成
-- 
-- 当 grade-service 启动时，DataInitializer 会自动：
-- 1. 生成100个连续编号的学生（S001-S100）
-- 2. 为每个学生生成完整的成绩数据（包括平时成绩、期中成绩、实验成绩、期末成绩）
-- 3. 按公式计算总成绩：平时×0.1 + 期中×0.2 + 实验×0.2 + 期末×0.5
-- 4. 生成用户账号（密码都是123456）
-- 5. 生成往期学期成绩数据
--
-- 如需重新生成数据，请：
-- 1. 清空数据库表（或删除数据库重新创建）
-- 2. 重新运行本SQL文件创建表结构
-- 3. 重启 grade-service，DataInitializer 会自动生成数据
-- 
-- ============================================
-- 完成！
-- ============================================
SELECT '数据库表结构创建完成！请启动 grade-service 自动生成测试数据。' AS message;


