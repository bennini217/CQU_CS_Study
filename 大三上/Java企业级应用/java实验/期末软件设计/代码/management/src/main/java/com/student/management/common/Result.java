package com.student.management.common;

import lombok.Data;

// 这是一个泛型类，用于封装所有接口的返回数据
@Data
public class Result<T> {
    private Integer code; // 状态码：200成功，500失败
    private String msg;   // 提示信息
    private T data;       // 具体数据（可能是用户对象，也可能是成绩列表）

    // 成功时的快捷方法
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    // 失败时的快捷方法
    public static Result error(String msg) {
        Result r = new Result();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}