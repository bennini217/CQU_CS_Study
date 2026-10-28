package com.student.grade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.common.entity.Student;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生Mapper接口
 * 成绩服务需要查询学生信息（用于显示学生姓名）
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}




