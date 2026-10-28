package com.student.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.management.common.Result;
import com.student.management.entity.Grade;
import com.student.management.entity.ScoreStats;
import com.student.management.entity.Student;
import com.student.management.mapper.GradeMapper;
import com.student.management.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private StudentMapper studentMapper;

    // 1. 获取全校排名 (Top 50)
    // 对应实验3的"全校排名总览"
    @GetMapping("/rank")
    public Result<List<Map<String, Object>>> getRank() {
        // 先查出所有成绩
        List<Grade> allGrades = gradeMapper.selectList(null);

        // 按学生ID和课程ID分组，避免重复计算同一门课程
        // 使用 Map<学生ID, Map<课程ID, 成绩>> 来去重
        Map<String, Map<String, BigDecimal>> studentCourseScores = new HashMap<>();
        for (Grade grade : allGrades) {
            String studentId = grade.getStudentId();
            String courseId = grade.getCourseId();
            
            // 如果该学生该课程还没有记录，或者新记录的成绩更高，则更新
            studentCourseScores.putIfAbsent(studentId, new HashMap<>());
            Map<String, BigDecimal> courseScores = studentCourseScores.get(studentId);
            
            // 如果该课程已有记录，保留更高的成绩（防止重复数据）
            if (!courseScores.containsKey(courseId) || 
                grade.getTotalScore().compareTo(courseScores.get(courseId)) > 0) {
                courseScores.put(courseId, grade.getTotalScore());
            }
        }

        // 计算每个学生的总分（所有课程成绩之和）和平均分
        Map<String, BigDecimal> studentTotalScores = new HashMap<>();
        Map<String, Integer> studentCourseCounts = new HashMap<>();
        
        for (Map.Entry<String, Map<String, BigDecimal>> entry : studentCourseScores.entrySet()) {
            String studentId = entry.getKey();
            Map<String, BigDecimal> courseScores = entry.getValue();
            
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal score : courseScores.values()) {
                total = total.add(score);
            }
            
            studentTotalScores.put(studentId, total);
            studentCourseCounts.put(studentId, courseScores.size());
        }

        // 转换为包含学生信息的列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : studentTotalScores.entrySet()) {
            String studentId = entry.getKey();
            Student student = studentMapper.selectById(studentId);
            if (student != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", student.getStudentId());
                item.put("name", student.getName());
                
                BigDecimal total = entry.getValue();
                int courseCount = studentCourseCounts.get(studentId);
                double avg = courseCount > 0 ? total.divide(BigDecimal.valueOf(courseCount), 2, RoundingMode.HALF_UP).doubleValue() : 0;
                
                item.put("total", total.doubleValue()); // 总分：所有课程成绩之和
                item.put("avg", avg); // 平均分
                item.put("courseCount", courseCount); // 课程数量
                
                result.add(item);
            }
        }

        // 按总分降序排序
        result.sort((a, b) -> Double.compare((Double) b.get("total"), (Double) a.get("total")));
        
        // 只取前50名
        if (result.size() > 50) {
            result = result.subList(0, 50);
        }

        return Result.success(result);
    }

    // 2. 获取成绩分布统计 (给 ECharts 饼图用)
    // 对应实验3的"多维度成绩分布统计"
    @GetMapping("/stats")
    public Result<List<ScoreStats>> getStats() {
        List<Grade> allGrades = gradeMapper.selectList(null);

        int excellent = 0; // 优秀 >= 90
        int good = 0;      // 良好 80-89
        int pass = 0;      // 及格 60-79
        int fail = 0;      // 不及格 < 60

        for (Grade g : allGrades) {
            // 注意：BigDecimal 比较大小需要用 compareTo
            double score = g.getTotalScore().doubleValue();
            if (score >= 90) excellent++;
            else if (score >= 80) good++;
            else if (score >= 60) pass++;
            else fail++;
        }

        // 封装成前端 ECharts 需要的格式: [{name: '优秀', value: 10}, ...]
        List<ScoreStats> stats = new ArrayList<>();
        stats.add(new ScoreStats("优秀 (90+)", excellent));
        stats.add(new ScoreStats("良好 (80-89)", good));
        stats.add(new ScoreStats("及格 (60-79)", pass));
        stats.add(new ScoreStats("不及格 (<60)", fail));

        return Result.success(stats);
    }

    // 3. 获取学业预警列表（不及格的学生）
    @GetMapping("/warning")
    public Result<List<Map<String, Object>>> getWarning() {
        List<Grade> fails = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .lt(Grade::getTotalScore, 60));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Grade grade : fails) {
            Student student = studentMapper.selectById(grade.getStudentId());
            if (student != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("studentId", grade.getStudentId());
                item.put("studentName", student.getName());
                item.put("courseName", grade.getCourseName());
                item.put("score", grade.getTotalScore().doubleValue());
                result.add(item);
            }
        }

        return Result.success(result);
    }
}