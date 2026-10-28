package com.student.management.entity;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ScoreStats {
    private String name;  // 类别名称，例如 "优秀"
    private Integer value; // 人数，例如 15
}