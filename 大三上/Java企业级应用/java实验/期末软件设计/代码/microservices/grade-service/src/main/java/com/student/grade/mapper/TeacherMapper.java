package com.student.grade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.common.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师Mapper接口
 * 成绩服务需要查询教师信息（用于课程表显示教师姓名）
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
}




