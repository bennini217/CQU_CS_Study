# 学生成绩管理系统 - 微服务版本

## 项目说明

本项目是将原有的单体应用（实验4）改造为分布式微服务架构的版本。

### 服务列表

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| gateway-service | 8080 | API网关（统一入口） |
| user-service | 8081 | 用户认证服务 |
| student-service | 8082 | 学生服务 |
| grade-service | 8083 | 成绩服务 |
| teacher-service | 8084 | 教师服务 |
| ai-service | 8085 | AI服务 |

---

## 环境要求

- **JDK**: 17或更高版本
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Nacos**: 2.3.0（服务注册中心）

---

## 快速开始

### 第一步：安装Nacos

1. **下载Nacos**
   - 访问：https://nacos.io/zh-cn/docs/quick-start.html
   - 下载Nacos 2.3.0（Windows版本）

2. **启动Nacos**
   ```bash
   # Windows
   cd nacos/bin
   startup.cmd -m standalone
   
   # Linux/Mac
   cd nacos/bin
   sh startup.sh -m standalone
   ```

3. **访问Nacos控制台**
   - 地址：http://localhost:8848/nacos
   - 默认账号/密码：`nacos` / `nacos`

### 第二步：创建数据库

1. **创建数据库**
   ```sql
   CREATE DATABASE IF NOT EXISTS student_db 
   DEFAULT CHARACTER SET utf8mb4 
   COLLATE utf8mb4_unicode_ci;
   USE student_db;
   ```

2. **创建数据表**
   
   详见 `数据库说明.md` 文件，包含所有建表SQL语句。

3. **初始化数据**
   
   可以手动插入测试数据，或使用原有的DataInitializer（需要适配微服务架构）。

### 第三步：修改配置

修改各服务的 `application.yml` 文件中的数据库密码：

```yaml
spring:
  datasource:
    password: 000000  # 改为您的MySQL密码
```

### 第四步：编译项目

```bash
cd 代码/microservices
mvn clean install
```

### 第五步：启动服务

**启动顺序**：
1. 先启动Nacos（已在第一步完成）
2. 启动各个微服务（顺序无关，但建议先启动业务服务，最后启动网关）
3. 最后启动gateway-service

**启动方式**：

#### 方式一：使用IDE启动（推荐）

在IDE中分别运行各个服务的 `*Application.java` 主类：
- `UserServiceApplication`
- `StudentServiceApplication`
- `GradeServiceApplication`
- `TeacherServiceApplication`
- `AIServiceApplication`
- `GatewayServiceApplication`

#### 方式二：使用命令行启动

```bash
# 在各个服务目录下执行
cd user-service
mvn spring-boot:run

# 新开终端窗口
cd student-service
mvn spring-boot:run

# ... 依此类推
```

#### 方式三：打包后启动

```bash
# 打包
mvn clean package

# 启动各个服务
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar student-service/target/student-service-1.0.0-SNAPSHOT.jar
java -jar grade-service/target/grade-service-1.0.0-SNAPSHOT.jar
java -jar teacher-service/target/teacher-service-1.0.0-SNAPSHOT.jar
java -jar ai-service/target/ai-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar
```

### 第六步：验证服务

1. **检查Nacos服务注册**
   - 访问：http://localhost:8848/nacos
   - 进入"服务管理" -> "服务列表"
   - 应该能看到所有6个服务已注册

2. **测试API**
   - 所有请求都通过网关访问：http://localhost:8080
   - 测试登录：`POST http://localhost:8080/api/login`
   - 前端访问地址不变：`http://localhost:8080/api/...`

---

## 项目结构

```
microservices/
├── pom.xml                      # 父POM
├── common/                      # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/student/common/
│       ├── Result.java          # 统一响应结果
│       ├── dto/                 # DTO类
│       ├── entity/              # 实体类
│       └── util/                # 工具类
├── user-service/                # 用户认证服务
├── student-service/             # 学生服务
├── grade-service/               # 成绩服务
├── teacher-service/             # 教师服务
├── ai-service/                  # AI服务
├── gateway-service/             # API网关
└── README.md                    # 本文件
```

---

## API路由说明

所有API请求都通过网关（8080端口）访问，网关会自动路由到对应的服务：

| 路径 | 路由到服务 | 说明 |
|------|-----------|------|
| `/api/login` | user-service | 登录 |
| `/api/change-password` | user-service | 修改密码 |
| `/api/student/**` | student-service | 学生相关 |
| `/api/grade/**` | grade-service | 成绩相关 |
| `/api/dashboard/**` | grade-service | 仪表盘 |
| `/api/teacher/**` | teacher-service | 教师相关 |
| `/api/ai/**` | ai-service | AI相关 |

---

## 服务间调用

### OpenFeign调用示例

`grade-service` 通过OpenFeign调用 `student-service`：

```java
@FeignClient(name = "student-service", path = "/api/student")
public interface StudentServiceClient {
    @GetMapping("/{studentId}")
    Result<Student> getStudent(@PathVariable String studentId);
}
```

---

## 前端配置

前端代码无需修改，只需要确保：
- API基础URL：`http://localhost:8080/api`（通过网关访问）
- 所有功能保持不变

---

## 常见问题

### 1. 服务启动失败：Nacos连接不上

**解决**：
- 确保Nacos已启动：访问 http://localhost:8848/nacos
- 检查 `application.yml` 中的 `spring.cloud.nacos.discovery.server-addr` 配置

### 2. 服务注册失败

**解决**：
- 检查Nacos是否正常运行
- 检查服务端口是否被占用
- 查看服务启动日志中的错误信息

### 3. 数据库连接失败

**解决**：
- 检查MySQL是否启动
- 检查 `application.yml` 中的数据库配置（用户名、密码、URL）
- 确认数据库 `student_db` 已创建

### 4. 网关路由失败

**解决**：
- 确保目标服务已启动并注册到Nacos
- 检查 `gateway-service` 的 `application.yml` 路由配置
- 查看网关日志

### 5. 前端跨域问题

**解决**：
- 网关已配置CORS，支持跨域
- 如果仍有问题，检查 `CorsConfig.java` 配置

---

## 技术栈

- **Spring Boot**: 3.2.5
- **Spring Cloud**: 2023.0.0
- **Spring Cloud Alibaba**: 2022.0.0.0
- **Nacos**: 2.3.0（服务注册与发现）
- **Spring Cloud Gateway**: API网关
- **OpenFeign**: 服务间HTTP调用
- **MyBatis-Plus**: 3.5.5
- **MySQL**: 8.0+
- **Java**: 17

---

## 开发说明

### 添加新服务

1. 在父POM的 `<modules>` 中添加新模块
2. 创建新服务的目录和 `pom.xml`
3. 在网关的 `application.yml` 中添加路由配置
4. 启动新服务并注册到Nacos

### 修改服务端口

在对应服务的 `application.yml` 中修改 `server.port`

---

## 注意事项

1. **数据库**：所有服务共享同一个数据库（适合演示），生产环境建议独立数据库
2. **服务启动顺序**：业务服务先启动，网关最后启动
3. **Nacos必须启动**：所有服务都依赖Nacos进行服务发现
4. **端口占用**：确保8080-8085端口未被占用

---

## 作者

段雯菀 - 2025-01

---

## 许可证

本项目仅用于课程作业。




