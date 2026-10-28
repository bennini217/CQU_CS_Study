# SQL注入防护实现说明

## 1. 防护策略概述

本系统采用多层防护策略，从根源上防止SQL注入攻击：

1. **参数化查询**：使用MyBatis-Plus的LambdaQueryWrapper实现
2. **输入参数过滤**：对用户输入进行特殊字符检测和过滤
3. **数据库权限控制**：应用程序数据库账号仅分配必要权限

## 2. 核心实现

### 2.1 参数化查询（主要防护手段）

**安全代码示例：**

```java
// ✅ 安全：使用LambdaQueryWrapper参数化查询
List<Grade> gradeList = gradeMapper.selectList(
    new LambdaQueryWrapper<Grade>()
        .eq(Grade::getStudentId, studentId)  // 参数自动预编译，避免注入
);
```

**生成的SQL：**
```sql
SELECT * FROM grade WHERE student_id = ?  -- 参数化，安全
```

**禁止方案（本系统未使用）：**
```java
// ❌ 不安全：手动拼接SQL（存在注入风险）
// String sql = "SELECT * FROM grade WHERE student_id = '" + studentId + "'";
// List<Grade> gradeList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Grade.class));
```

### 2.2 输入参数过滤

**工具类：`SqlInjectionProtection`**

提供以下方法：
- `isSafe(String input)`：检查字符串是否包含危险字符
- `filter(String input)`：过滤危险字符
- `validateAndClean(String input, String fieldName)`：验证并清理输入
- `validateUserId(String userId)`：验证用户ID格式和安全性
- `validatePassword(String password)`：验证密码安全性

**危险字符列表：**
- SQL关键字：`select`, `insert`, `update`, `delete`, `drop`, `alter`, `create`, `exec`, `execute`, `union`
- 特殊字符：`'`, `"`, `;`, `--`, `/*`, `*/`
- 其他：`xp_`, `sp_`, `script`, `<`, `>`, `&`, `|`, `&&`, `||`

**使用示例：**
```java
// 在Controller中使用
String safeStudentId;
try {
    safeStudentId = SqlInjectionProtection.validateUserId(studentId);
} catch (IllegalArgumentException e) {
    return Result.error(e.getMessage());
}

// 使用安全参数进行查询
List<Grade> list = gradeMapper.selectList(
    new LambdaQueryWrapper<Grade>()
        .eq(Grade::getStudentId, safeStudentId)
);
```

## 3. 已实现的防护点

### 3.1 登录接口（LoginController）
- ✅ 用户ID格式验证（S或T开头+3位数字）
- ✅ 密码安全性验证
- ✅ 参数化查询用户信息

### 3.2 成绩查询接口（GradeController）
- ✅ 学生ID参数过滤
- ✅ 班级ID参数过滤
- ✅ 教师ID参数过滤
- ✅ 所有查询使用LambdaQueryWrapper

### 3.3 AI智能接口（AIController）
- ✅ 学生ID参数过滤
- ✅ 问题内容参数过滤
- ✅ 参数化查询成绩数据

### 3.4 仪表盘接口（DashboardController）
- ✅ 学生ID参数过滤
- ✅ 教师ID参数过滤
- ✅ 参数化查询统计数据

## 4. 数据库权限控制建议

### 4.1 创建专用数据库账号

```sql
-- 创建应用程序专用账号
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'strong_password';

-- 仅分配必要权限（SELECT、INSERT、UPDATE）
GRANT SELECT, INSERT, UPDATE ON student_db.* TO 'app_user'@'localhost';

-- 不分配危险权限（DROP、ALTER、CREATE等）
-- GRANT DROP, ALTER, CREATE ON student_db.* TO 'app_user'@'localhost';  -- ❌ 禁止

-- 刷新权限
FLUSH PRIVILEGES;
```

### 4.2 修改application.properties

```properties
# 使用专用账号，而非root账号
spring.datasource.username=app_user
spring.datasource.password=strong_password
```

## 5. 防护效果

### 5.1 攻击示例（已被防护）

**攻击尝试1：SQL注入**
```
输入：studentId = "S001' OR '1'='1"
结果：被validateUserId()拦截，抛出异常
```

**攻击尝试2：SQL关键字注入**
```
输入：studentId = "S001; DROP TABLE grade; --"
结果：被isSafe()检测到危险字符，拒绝请求
```

**攻击尝试3：参数化查询保护**
```
即使攻击者绕过前端验证，参数化查询也会将恶意输入作为普通字符串处理
生成的SQL：SELECT * FROM grade WHERE student_id = ?
参数值：'S001' OR '1'='1'（作为字符串，不会执行）
```

## 6. 最佳实践

1. **始终使用参数化查询**
   - 使用LambdaQueryWrapper而非字符串拼接
   - 避免使用`@Select`注解直接写SQL（除非使用`#{}`占位符）

2. **输入验证和过滤**
   - 在Controller层进行参数验证
   - 使用工具类统一处理危险字符

3. **最小权限原则**
   - 数据库账号仅分配必要权限
   - 定期审查权限配置

4. **日志记录**
   - 记录所有异常输入尝试
   - 监控可疑的数据库查询模式

## 7. 测试建议

### 7.1 功能测试
- 正常输入：验证功能正常
- 边界值测试：空值、超长字符串等

### 7.2 安全测试
- SQL注入测试：尝试各种SQL注入payload
- 特殊字符测试：单引号、分号、注释符等
- 格式验证测试：错误的用户ID格式

### 7.3 测试用例示例

```java
// 测试用例1：SQL注入尝试
@Test
public void testSqlInjection() {
    String maliciousInput = "S001' OR '1'='1";
    assertThrows(IllegalArgumentException.class, () -> {
        SqlInjectionProtection.validateUserId(maliciousInput);
    });
}

// 测试用例2：正常输入
@Test
public void testNormalInput() {
    String normalInput = "S001";
    String result = SqlInjectionProtection.validateUserId(normalInput);
    assertEquals("S001", result);
}
```

## 8. 总结

本系统通过以下三层防护机制，有效防止SQL注入攻击：

1. **参数化查询**：MyBatis-Plus的LambdaQueryWrapper自动使用预编译语句
2. **输入过滤**：SqlInjectionProtection工具类检测和过滤危险字符
3. **权限控制**：数据库账号最小权限原则

这些措施从根源上杜绝了SQL注入风险，保障了系统数据的安全性。
