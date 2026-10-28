package com.student.user.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.common.Result;
import com.student.common.dto.ChangePasswordDTO;
import com.student.common.dto.LoginDTO;
import com.student.common.entity.User;
import com.student.common.util.PasswordUtil;
import com.student.common.util.SqlInjectionProtection;
import com.student.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户认证控制器
 * 负责用户登录和密码管理功能
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@RestController
@RequestMapping("/api")
@Tag(name = "用户认证模块", description = "登录与密码管理")
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    /**
     * 用户登录接口
     * 使用@Valid注解进行参数校验，结合DTO中的校验注解
     * 
     * @param loginDTO 登录请求DTO
     * @param bindingResult 校验结果
     * @return 登录结果，包含token、用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持学生(S开头)和教师(T开头)登录，密码默认123456，密码长度不小于6位")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO, BindingResult bindingResult) {
        System.out.println("=== 收到登录请求 ===");
        
        // 参数校验：如果校验失败，返回错误信息
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            return Result.error(errorMsg);
        }
        
        // 基础校验（双重保障，防止校验注解被绕过）
        if (StrUtil.isBlank(loginDTO.getUsername()) || StrUtil.isBlank(loginDTO.getPassword())) {
            return Result.error("用户名或密码不能为空");
        }
        
        // 格式校验：用户名必须以S或T开头，后跟3位数字
        if (!loginDTO.getUsername().matches("^[ST]\\d{3}$")) {
            return Result.error("用户名格式错误，应为S或T开头后跟3位数字（如：S001、T001）");
        }
        
        // 密码长度校验
        if (loginDTO.getPassword().length() < 6) {
            return Result.error("密码长度不能小于6位");
        }
        
        // SQL注入防护：验证并清理用户输入
        String safeUsername;
        String safePassword;
        try {
            safeUsername = SqlInjectionProtection.validateUserId(loginDTO.getUsername());
            safePassword = SqlInjectionProtection.validatePassword(loginDTO.getPassword());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询（LambdaQueryWrapper），防止SQL注入
        // 所有参数通过预编译方式传入，不会进行SQL拼接
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, safeUsername));
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证密码
        // 演示模式：明文比对；生产模式：BCrypt加密验证
        if (!passwordUtil.verify(safePassword, user.getPassword())) {
            return Result.error("密码错误");
        }
        
        // 生成响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", UUID.randomUUID().toString());
        data.put("name", user.getName());
        // 前端期望role为'student'或'teacher'，而不是'STUDENT'或'TEACHER'
        String role = "STUDENT".equals(user.getUserType()) ? "student" : "teacher";
        data.put("role", role);
        data.put("id", user.getUsername()); // 注意：前端通常用 S001 作为 ID

        return Result.success(data);
    }

    /**
     * 修改密码接口
     * 使用@Valid注解进行参数校验
     * 
     * @param dto 修改密码DTO
     * @param bindingResult 校验结果
     * @return 修改结果
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改用户密码，新密码长度不小于6位")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto, BindingResult bindingResult) {
        System.out.println("=== 收到修改密码请求 ===");
        
        // 参数校验：如果校验失败，返回错误信息
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            return Result.error(errorMsg);
        }
        
        // 基础校验（双重保障）
        if (StrUtil.isBlank(dto.getId()) || StrUtil.isBlank(dto.getOldPass()) || StrUtil.isBlank(dto.getNewPass())) {
            return Result.error("参数不能为空");
        }
        
        // 新密码长度校验
        if (dto.getNewPass().length() < 6) {
            return Result.error("新密码长度不能小于6位");
        }
        
        // SQL注入防护：验证并清理用户输入
        String safeId;
        String safeOldPass;
        String safeNewPass;
        try {
            safeId = SqlInjectionProtection.validateUserId(dto.getId());
            safeOldPass = SqlInjectionProtection.validatePassword(dto.getOldPass());
            safeNewPass = SqlInjectionProtection.validatePassword(dto.getNewPass());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询（LambdaQueryWrapper），防止SQL注入
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, safeId));
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证旧密码
        // 演示模式：明文比对；生产模式：BCrypt加密验证
        if (!passwordUtil.verify(safeOldPass, user.getPassword())) {
            return Result.error("旧密码错误");
        }

        // 更新密码
        // 演示模式：明文存储；生产模式：BCrypt加密后存储
        String encryptedNewPassword = passwordUtil.encrypt(safeNewPass);
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);

        return Result.success("密码修改成功");
    }
}




