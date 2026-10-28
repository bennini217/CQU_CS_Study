package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求DTO
 * 使用JSR-303校验注解进行参数校验
 */
@Data
public class LoginDTO {
    
    @NotBlank(message = "用户名或学号/工号不能为空")
    @Pattern(regexp = "^[ST]\\d{3}$", message = "用户名格式错误，应为S或T开头后跟3位数字（如：S001、T001）")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能小于6位")
    private String password;
}
