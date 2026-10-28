package com.student.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 * 用于封装所有接口的返回数据
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
public class Result<T> {
    private Integer code; // 状态码：200成功，500失败
    private String msg;   // 提示信息
    private T data;       // 具体数据（可能是用户对象，也可能是成绩列表）

    /**
     * 成功时的快捷方法
     * 
     * @param data 返回的数据
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    /**
     * 失败时的快捷方法
     * 
     * @param msg 错误信息
     * @return Result对象
     */
    public static Result error(String msg) {
        Result r = new Result();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}




