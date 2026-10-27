# 环境配置说明

## 1. 支持环境

| 组件 | 要求 |
| --- | --- |
| 操作系统 | Windows 10/11；Linux/macOS 也可运行 Java 与 Maven |
| JDK | Java 17 |
| Maven | Maven 3.10 或兼容版本 |
| 数据库 | H2 2.x 文件模式，由项目依赖自动提供 |
| 浏览器 | Edge、Chrome、Firefox 等现代浏览器 |
| Node.js | 22.x（仅构建或开发 React 前端时需要） |

当前开发电脑的参考路径：

```text
JDK: D:\Project\jdk
Maven: D:\Project\apache-maven-3.10.0
```

检查命令：

```powershell
& "D:\Project\jdk\bin\java.exe" -version
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" -v
```

## 2. 创建本地环境文件

模板文件：

```text
.env.example
```

复制为本地配置：

```powershell
cd D:\workspace\Web\review-desk
Copy-Item .env.example .env
notepad .env
```

`.env` 已加入 `.gitignore`，不会被提交到 GitHub。

`start-local.ps1` 和 `start-public.ps1` 会自动读取 `.env` 并写入当前进程环境。直接使用 `java -jar` 或 IDE 启动时，需要手动设置环境变量或在 IDE 运行配置中导入。

环境模板中的值不需要额外添加引号。

使用启动脚本加载配置并前台运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-local.ps1
```

## 3. 环境变量

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP 服务端口 |
| `SERVER_ADDRESS` | `0.0.0.0` | 监听地址；局域网访问时保持默认 |
| `FORWARD_HEADERS_STRATEGY` | `framework` | 正确识别 Nginx 转发的协议、域名和客户端 IP |
| `DATABASE_URL` | `jdbc:h2:file:./data/review-desk` | H2 文件数据库地址 |
| `DATABASE_USERNAME` | `sa` | 数据库用户名 |
| `DATABASE_PASSWORD` | 空字符串 | 数据库密码 |
| `JPA_DDL_AUTO` | `update` | Hibernate 表结构更新策略 |
| `SESSION_COOKIE_SECURE` | `false` | 是否只允许 HTTPS 发送会话 Cookie |
| `SESSION_COOKIE_SAME_SITE` | `lax` | 会话 Cookie 的 SameSite 策略 |
| `SPRING_APPLICATION_NAME` | `review-desk` | 应用名称 |
| `THYMELEAF_CACHE` | `false` | 是否缓存服务端页面模板 |
| `AI_API_KEY` | 空 | OpenAI 兼容模型密钥；为空时使用规则生成 |
| `AI_BASE_URL` | `https://api.deepseek.com` | 模型 API 基地址 |
| `AI_MODEL` | `deepseek-chat` | 模型名称 |
| `AI_TIMEOUT_SECONDS` | `30` | 模型请求超时秒数 |
| `AI_SETTINGS_SECRET` | 本地开发默认值 | 加密教师在页面中填写的 API Key；生产必须更换 |
| `AI_BATCH_SIZE` | `3` | 每轮自动预审最多处理的提交数 |
| `AI_BATCH_INITIAL_DELAY_MS` | `15000` | 服务启动后首次扫描等待时间 |
| `AI_BATCH_DELAY_MS` | `60000` | 自动预审扫描间隔 |
| `SHOW_DEMO_CREDENTIALS` | `true` | 登录页是否显示初始演示账号；生产环境应设为 `false` |
| `DEMO_TEACHER_USERNAME` | `chen` | 首次启动创建的教师账号 |
| `DEMO_TEACHER_PASSWORD` | `Teach1234` | 首次启动创建的教师密码 |
| `DEMO_TEACHER_NAME` | `陈老师` | 演示教师姓名 |
| `DEMO_STUDENT_USERNAME` | `lin` | 首次启动创建的学生账号 |
| `DEMO_STUDENT_PASSWORD` | `Student1234` | 首次启动创建的学生密码 |
| `DEMO_STUDENT_NAME` | `林清和` | 演示学生姓名 |
| `DEMO_STUDENT_NUMBER` | `20230116` | 演示学生学号 |

## 4. 推荐开发配置

```dotenv
SERVER_PORT=8080
SERVER_ADDRESS=0.0.0.0
DATABASE_URL=jdbc:h2:file:./data/review-desk
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
JPA_DDL_AUTO=update
SESSION_COOKIE_SECURE=false
SESSION_COOKIE_SAME_SITE=lax
SPRING_APPLICATION_NAME=review-desk
THYMELEAF_CACHE=false
AI_API_KEY=
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat
AI_TIMEOUT_SECONDS=30
AI_SETTINGS_SECRET=change-this-local-encryption-secret

DEMO_TEACHER_USERNAME=chen
DEMO_TEACHER_PASSWORD=Teach1234
DEMO_TEACHER_NAME=陈老师
DEMO_STUDENT_USERNAME=lin
DEMO_STUDENT_PASSWORD=Student1234
DEMO_STUDENT_NAME=林清和
DEMO_STUDENT_NUMBER=20230116
```

## 5. 公网展示配置

公开访问前至少修改：

```dotenv
DEMO_TEACHER_PASSWORD=替换为高强度密码
DEMO_STUDENT_PASSWORD=替换为高强度密码
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=lax
THYMELEAF_CACHE=true
AI_API_KEY=替换为服务端模型密钥
AI_SETTINGS_SECRET=替换为长随机加密密钥
```

注意：

- 演示账号只在数据库不存在对应账号时创建。
- 修改演示账号环境变量后，如果旧数据库仍存在，已有账号不会自动改变。
- 如需重新应用新的演示账号，请先停止服务并备份、删除 `data/review-desk.mv.db`。
- `SESSION_COOKIE_SECURE=true` 要求浏览器通过 HTTPS 访问。

## 6. 数据库切换

H2 文件模式默认配置：

```dotenv
DATABASE_URL=jdbc:h2:file:./data/review-desk
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
```

H2 内存模式可用于临时调试：

```dotenv
DATABASE_URL=jdbc:h2:mem:review-desk-dev
JPA_DDL_AUTO=create-drop
```

内存模式在进程结束后会丢失全部数据。

## 7. 局域网访问

使用 `SERVER_ADDRESS=0.0.0.0` 启动后，可通过电脑 IP 访问：

```powershell
ipconfig
```

假设 IPv4 地址为 `192.168.1.20`，访问：

```text
http://192.168.1.20:8080
```

如果无法访问，请检查 Windows 防火墙是否允许 Java 的 TCP 入站连接，以及路由器是否开启了终端隔离。

## 8. 启动方式

本地前台启动：

```text
start-local.cmd
```

本地后台启动并生成临时公网地址：

```text
start-public.cmd
```

停止后台网站与隧道：

```text
stop-public.cmd
```

## 9. 测试配置

测试使用独立的 H2 内存数据库：

```text
src/test/resources/application.yml
```

测试不会修改本地 `data/review-desk.mv.db`。

React 前端检查与构建：

```powershell
cd frontend
npm ci
npm run lint
npm run build
```

运行测试：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" test
```

