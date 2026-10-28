package com.student.common.entity;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 成绩统计实体类
 * 用于返回成绩分布统计信息
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@AllArgsConstructor
public class ScoreStats {
    private String name;  // 类别名称，例如 "优秀"
    private Integer value; // 人数，例如 15
}




