package com.student.grade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 用于数据初始化
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 已经内置了 CRUD 方法
}








