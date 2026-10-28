package com.student.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.management.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    // 这里暂时不需要写代码，MyBatis-Plus 已经内置了 CRUD
}