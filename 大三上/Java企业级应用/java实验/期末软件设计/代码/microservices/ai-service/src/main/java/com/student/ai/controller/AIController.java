package com.student.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.common.Result;
import com.student.common.annotation.RequireAuth;
import com.student.common.annotation.RequireRole;
import com.student.common.entity.Grade;
import com.student.common.util.SqlInjectionProtection;
import com.student.ai.mapper.GradeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI控制器
 * 负责AI智能问答功能
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private GradeMapper gradeMapper;

    /**
     * AI智能问答接口
     * 
     * @param request 请求参数（包含question和studentId）
     * @return AI回答
     */
    @PostMapping("/chat")
    @RequireAuth
    @RequireRole("student")
    public Result<String> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String studentId = request.get("studentId");

        // 简单的判空保护
        if (studentId == null || question == null) {
            return Result.error("参数不完整，需要 studentId 和 question");
        }

        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        String safeQuestion;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
            safeQuestion = SqlInjectionProtection.validateAndClean(question, "question");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        System.out.println("🤖 AI 收到 " + safeStudentId + " 的提问: " + safeQuestion);

        // 1. 挂科查询
        if (safeQuestion.contains("挂科") || safeQuestion.contains("不及格")) {
            // 使用参数化查询，防止SQL注入
            List<Grade> fails = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                    .eq(Grade::getStudentId, safeStudentId)
                    .lt(Grade::getTotalScore, 60));

            if (fails.isEmpty()) {
                return Result.success("🎉 恭喜你！经数据库核查，你目前没有挂科记录，请继续保持！");
            } else {
                String courses = fails.stream().map(Grade::getCourseName).collect(Collectors.joining("、"));
                return Result.success("⚠️ 警报：检测到你有 " + fails.size() + " 门课程不及格 (" + courses + ")。请尽快联系老师！");
            }
        }

        // 2. 平均分查询
        if (safeQuestion.contains("平均分") || safeQuestion.contains("成绩")) {
            // 使用参数化查询，防止SQL注入
            List<Grade> grades = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                    .eq(Grade::getStudentId, safeStudentId));
            if (grades.isEmpty()) return Result.success("暂无成绩记录。");

            double avg = grades.stream()
                    .mapToDouble(g -> g.getTotalScore().doubleValue())
                    .average().orElse(0.0);

            return Result.success(String.format("📊 你的加权平均分是 %.2f 分。继续加油！", avg));
        }

        // 3. 学习建议
        if (question.contains("建议")) {
            return Result.success("💡 学习建议：根据你的成绩数据，平时成绩权重较高，请务必保持良好的出勤率；期末复习时请重点关注 '数据结构' 等核心专业课。");
        }

        // 4. 兜底回复
        return Result.success("我是你的智能助教。你可以问我：'我挂科了吗？' 或 '我的平均分是多少？'");
    }
}




