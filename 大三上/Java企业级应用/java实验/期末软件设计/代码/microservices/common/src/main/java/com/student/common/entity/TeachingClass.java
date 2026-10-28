package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 教学班实体类
 * 对应数据库表：teaching_class
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("teaching_class")
public class TeachingClass {
    @TableId
    private String classId;
    private String courseId;
    private String teacherId;
    private String semester;
    private String dayOfWeek;
    private String timePeriod;
}




