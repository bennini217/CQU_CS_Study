package com.student.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.management.entity.TeachingClass; // 确保引入了这个
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeachingClassMapper extends BaseMapper<TeachingClass> { // ✅ 重点：这里必须是 TeachingClass
}