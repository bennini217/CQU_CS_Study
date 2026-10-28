package com.student.management.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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