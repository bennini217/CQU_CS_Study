## 项目简介
- Java Socket 聊天系统，支持 TCP 客户端(Swing) 与 Web 前端（HTML/CSS/JS）通过嵌入式 HTTP/WebSocket 网关互通，文字/文件/图片、一对一与群发、实时在线列表。
- 纯 Java 后端，无外部依赖；前端纯原生三件套，内置随机用户名与本地生成的 emoji 头像气泡。

## 目录结构
- `src/main/java/com/example/chat/` 核心代码（ChatServer、WebSocketGatewayServer、GatewayMain、消息模型等）
- `web/` 前端静态资源（index.html, app.js）
- `out/` 编译输出目录（javac 生成）

## 环境准备
- JDK 17+（Mac/Win/Linux 均可）
- 浏览器（Chrome/Edge/Firefox 等）
- python3（可选，用于批量编译脚本；也可直接用 javac）

## 编译与运行
1) 编译（已存在 `out/` 可跳过）
```bash
cd /Users/xxxxx/dev/计网
python3 - <<'PY'
import pathlib, subprocess
base = pathlib.Path('src/main/java')
files = [str(p) for p in base.rglob('*.java')]
subprocess.check_call(['javac', '-d', 'out', *files])
print("编译完成，输出目录 out")
PY
```
（或手动：`find src/main/java -name "*.java" -print0 | xargs -0 javac -d out`）

2) 启动统一服务（TCP + HTTP/WebSocket 网关）
```bash
cd /Users/xxxxx/dev/计网
java -cp out com.example.chat.GatewayMain 5000 8080
```
   - TCP 聊天端口：5000
   - Web/WS 端口：8080

3) Web 端体验
- 打开浏览器访问 `http://localhost:8080`
- 页面自动分配随机用户名（格式 `用户_xxx`）并自动连接，可直接聊天。

4) 原生 Swing 客户端（可选）
```bash
java -cp out com.example.chat.ClientMain
```
输入服务器地址/端口/用户名后进入聊天界面。Swing 客户端与 Web 前端互通。

## 项目原理精讲
### 协议与消息模型
- 使用可序列化的 `ChatMessage`（字段：type/from/targets/text/fileName/data/onlineUsers/note/timestamp），枚举 `MessageType` 覆盖 LOGIN/LOGIN_OK/LOGIN_FAILED/USER_LIST/CHAT/FILE/IMAGE/SYSTEM/LOGOUT/HEARTBEAT。
- TCP 客户端与服务器之间用 ObjectInput/OutputStream 直接传输 `ChatMessage`。
- WebSocket 网关使用轻量文本帧协议：URL 查询串格式 `key=value&key=value`，文件/图片 data 使用 Base64；在网关侧进行 decode/encode 再转发为 `ChatMessage`。

### 服务器与网关
- `ChatServer`：维护在线用户映射 `user -> ChatSession`（抽象会话接口）；提供注册、注销、用户列表广播、系统广播、消息转发（群发或定向）。
- TCP 侧：`ClientHandler` 实现 `ChatSession`，负责登录校验、消息转发、下线清理。
- Web 侧：`WebSocketGatewayServer` 内置极简 HTTP 服务器（提供 `/` 与 `/app.js`），并在 `/ws` 升级为 WebSocket。支持大负载/分片帧的文本组装，消息进入后转换为 `ChatMessage` 交由 `ChatServer`。
- `GatewayMain` 同时启动 ChatServer 与 WebSocketGatewayServer，形成“一进程双入口”。

### 前端实现要点
- 纯原生 JS：自动生成随机用户名，自动连接；在线用户列表多选=组聊，空选=群发。
- 气泡 UI：flex 布局，自己消息右对齐、他人消息左对齐；系统消息居中纯文本。头像由用户名哈希生成彩色底+Emoji，本地 Canvas 渲染，无外部依赖。
- 发送文件/图片：读为 Base64，类型区分 FILE/IMAGE，接收端生成 data:URL 供下载/预览；发送者本地即时渲染，无需等待回显。

### 大文件与可靠性
- WebSocket 读取实现了分片帧组装，避免大文件/图片导致连接被关闭。
- 服务器在用户上下线时广播用户列表，客户端实时刷新；发送失败时会话关闭并触发注销。

## 常见问题
- 端口被占用：修改 `GatewayMain` 启动参数，例如 `java -cp out com.example.chat.GatewayMain 5001 8081`。
- 头像不显示：检查浏览器是否支持 Emoji 彩色字体；本实现为本地 Canvas 渲染，不依赖外网。
- 大文件过大：当前通过 Base64 传输，建议控制文件大小；如需更大文件可扩展为分片或外部存储。

## Windows 快速上手
环境：已安装 JDK 17+，可用 PowerShell 或 cmd。

1) 打开 PowerShell，进入项目目录（示例路径请替换为你的实际位置）：
```powershell
cd C:\Users\yourname\dev\计网
```

2) 编译：
```powershell
Get-ChildItem -Recurse src\main\java\*.java | ForEach-Object { $_.FullName } |
  javac -d out @-
```
或使用纯 cmd：
```cmd
cd C:\Users\yourname\dev\计网
for /r %f in (src\main\java\*.java) do @echo %f >> sources.txt
javac -d out @sources.txt
del sources.txt
```

3) 运行服务器 + 网关：
```cmd
cd C:\Users\yourname\dev\计网
java -cp out com.example.chat.GatewayMain 5000 8080
```

4) Web 端体验：浏览器访问 `http://localhost:8080`，页面会自动分配随机用户名并连接。

5) Swing 客户端（可选）：
```cmd
java -cp out com.example.chat.ClientMain
```
在弹窗中填入服务器地址、端口与用户名即可。**
