package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 * 使用JSR-303校验注解进行参数校验
 */
@Data
public class ChangePasswordDTO {
    
    @NotBlank(message = "用户ID不能为空")
    private String id;
    
    @NotBlank(message = "旧密码不能为空")
    private String oldPass;
    
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度不能小于6位")
    private String newPass;
}
