package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩实体类
 * 对应数据库表：grade
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@TableName("grade")
public class Grade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentId;
    private String classId;    // 关联教学班
    private String courseId;   // 冗余方便查询
    private String courseName; // 冗余方便查询

    // 分项成绩
    private Integer normalScore;
    private Integer midtermScore;
    private Integer labScore;
    private Integer finalScore;
    private BigDecimal totalScore; // 综合成绩

    private String semester;
    private LocalDateTime recordTime;
}




