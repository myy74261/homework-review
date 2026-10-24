# 明鉴 · ReviewDesk

面向“智慧教育”场景的教师备课辅助与作业审核平台。项目连接教师发布作业、学生提交、教师审核、共性问题汇总、课堂讲解建议和备课素材导出，帮助教师减少重复整理工作，同时保留证据来源、审核记录和教师最终判断。

项目不进行 AI 作弊判定，不以模型概率判断学生是否独立完成作业。平台只整理教师确认的问题，所有课堂建议均由教师审核后使用。

## 项目目标

- 缩短教师从批阅作业到整理课堂共性问题的时间。
- 让教学反馈对应学生原文、评分量规和教师判断。
- 保留完整审核轨迹，支持版本对比与历史恢复。
- 明确区分学生原文、平台建议和教师最终意见。
- 形成“教师发布、学生提交、教师审核、备课输出”的教学闭环。

## 核心流程

```text
教师创建课堂
  → 学生使用课堂码加入
  → 教师发布作业和评分量规
  → 学生在线提交
  → 教师审核并确认问题
  → 汇总共性问题
  → 生成可编辑的课堂讲解建议
  → 导出备课材料
  → 完成审核并向学生发布最终反馈
```

## 功能概览

### 教师端

- 注册、登录和退出，只能访问自己的课堂数据。
- 创建课堂并生成唯一课堂码。
- 发布作业，配置四项总分 100 分的评分量规。
- 查看提交队列、审核状态和共性问题。
- 自动保存审核意见，检测版本冲突。
- 创建反馈版本，逐行对比并恢复历史反馈。
- 确认学生问题，保留原文证据和教师修改建议。
- 根据共性问题生成课堂目标、错例分析、追问和练习。
- 编辑并自动保存备课稿，支持重新生成。
- 导出 Word 兼容 `.doc` 和 Markdown 文档。
- 完成审核后向学生发布最终反馈。

### 学生端

- 注册、登录和退出。
- 使用教师提供的课堂码加入课堂。
- 查看教师发布的作业要求和评分量规。
- 在线提交和重新提交作业。
- 每次提交保留为独立记录。
- 审核完成后查看教师最终反馈。

## 技术架构

```text
浏览器
├── Thymeleaf 服务端渲染页面
└── 原生 JavaScript 自动保存与交互
        │
Spring MVC
├── 教师端、学生端及审核控制器
├── REST API
├── Spring Security 会话与角色权限
└── 全局异常处理
        │
业务服务层
├── 账号与课堂
├── 作业发布与学生提交
├── 审核意见与历史版本
├── 问题汇总与备课建议
└── 导出与访问边界校验
        │
Spring Data JPA
└── H2 文件数据库
```

| 模块 | 职责 |
| --- | --- |
| `domain` | 账号、课堂、作业、提交、审核、事件、问题与备课稿实体 |
| `repository` | JPA 仓储接口，集中声明数据访问方法 |
| `service` | 业务规则、权限边界、版本控制、问题汇总与建议生成 |
| `security` | Spring Security 用户加载、会话和角色路由 |
| `web` | 页面路由、表单、REST 控制器与 DTO |
| `bootstrap` | 首次启动时的演示账号、课堂和作业数据 |
| `templates` | 登录、教师端、学生端、审核和备课页面 |
| `static` | 响应式 CSS、自动保存和页面交互脚本 |

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 运行环境 | Java 17 |
| 后端 | Spring Boot 3.3.5、Spring MVC、Thymeleaf |
| 安全 | Spring Security 6.3、BCrypt、CSRF、角色权限 |
| 数据 | Spring Data JPA、Hibernate、H2（开发）、PostgreSQL（部署） |
| 前端 | HTML5、CSS3、原生 JavaScript |
| 构建 | Maven 3.10 |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、Spring Security Test |

## 项目结构

```text
review-desk/
├── src/main/java/com/reviewdesk
│   ├── ReviewDeskApplication.java
│   ├── domain/
│   ├── repository/
│   ├── service/
│   ├── security/
│   ├── bootstrap/
│   └── web/
│       └── api/
├── src/main/resources
│   ├── application.yml
│   ├── templates/
│   └── static/
│       ├── css/
│       └── js/
├── src/test/java/com/reviewdesk/web
├── docs/
│   ├── ENVIRONMENT.md             完整环境变量与配置说明
│   ├── DEPENDENCIES.md            依赖清单与升级方式
│   ├── DEPLOYMENT.md              云服务器与 Docker 部署说明
│   └── TEAM.md                    团队职责与验收范围
├── .env.example                   环境变量模板
├── .env.production.example        生产环境变量模板
├── Dockerfile                     多阶段 Java 构建镜像
├── docker-compose.yml             应用与 PostgreSQL 编排
├── deploy/nginx-review-desk.conf  Nginx 反向代理模板
├── data/                          本地 H2 数据文件（不提交）
├── start-local.cmd                本地启动
├── start-public.cmd               后台启动网站并生成公网地址
├── stop-public.cmd                停止网站和公网隧道
└── pom.xml
```

## 安装与运行

### 环境要求

- JDK 17
- Maven 3.10
- Windows PowerShell
- 浏览器

当前开发环境使用的工具路径：

- JDK：`D:\Project\jdk`
- Maven：`D:\Project\apache-maven-3.10.0`

检查环境：

```powershell
java -version
mvn -v
```

完整环境配置见 [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md)，依赖清单见 [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md)。

### 初始化环境变量

```powershell
cd D:\workspace\Web\review-desk
Copy-Item .env.example .env
notepad .env
```

`.env` 不会被 Git 提交。启动脚本会自动加载其中的配置。

### 获取项目

```powershell
git clone https://github.com/myy74261/homework-review.git
cd homework-review
```

### 开发模式启动

```powershell
start-local.cmd
```

`start-local.cmd` 会加载 `.env`、在缺少 JAR 时执行打包，并以前台模式运行网站。

开发热启动仍可使用：

```powershell
$env:JAVA_HOME = "D:\Project\jdk"
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

浏览器访问：

```text
http://localhost:8080
```

### 打包后启动

```powershell
cd D:\workspace\Web\review-desk
$env:JAVA_HOME = "D:\Project\jdk"
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" package
& "D:\Project\jdk\bin\java.exe" -jar "target\review-desk-0.1.0-SNAPSHOT.jar"
```

### 后台公网预览

双击 `start-public.cmd`。脚本会：

1. 在后台启动打包后的网站。
2. 启动 Cloudflare 临时隧道。
3. 输出并打开公网 HTTPS 地址。

停止网站和隧道时双击 `stop-public.cmd`。

> 临时公网地址没有可用性保证，电脑休眠、断网或关闭进程后都会失效。部分校园或企业网络可能无法访问 `trycloudflare.com`。

### 云服务器部署

项目已提供 Docker Compose + PostgreSQL 部署方案。

服务器首次部署：

```bash
cd /opt/review-desk
cp .env.production.example .env.production
chmod 600 .env.production
docker compose --env-file .env.production up -d --build
```

完整步骤见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)，包括：

- 生产环境变量和安全密码
- PostgreSQL 数据卷
- Nginx 反向代理
- HTTPS 证书
- 防火墙规则
- 数据库备份和版本更新

## 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 教师 | `chen` | `Teach1234` |
| 学生 | `lin` | `Student1234` |

首次启动时会自动创建演示账号，并建立“传播学方法课 · 2026 秋”课堂、学生成员关系和示例作业。

演示密码仅用于本地开发。公网展示或正式部署前必须修改密码。

## 数据与配置

数据文件：

```text
data/review-desk.mv.db
```

- `data/` 已加入 `.gitignore`，不会提交用户数据。
- H2 文件只允许一个 Java 进程占用，启动前应停止旧实例。
- 备份网站数据时复制该数据库文件即可。
- 清除演示数据时，先停止网站，再删除该文件；下次启动会自动重新初始化演示数据。

应用配置位于：

```text
src/main/resources/application.yml
```

## 测试与构建

运行全部测试：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" test
```

跳过测试打包：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" -DskipTests package
```

测试覆盖账号注册登录、角色隔离、课堂码加入、跨教师数据边界、作业发布、学生提交、审核保存、版本对比与恢复、问题汇总、备课生成编辑和文档导出。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/login` | 账号登录 |
| POST | `/register` | 注册教师或学生账号 |
| POST | `/teacher/classrooms` | 创建课堂 |
| POST | `/student/join` | 使用课堂码加入课堂 |
| POST | `/teacher/assignments` | 发布作业 |
| POST | `/student/assignments/{id}/submissions` | 提交作业 |
| GET/PUT | `/api/submissions/{id}/review` | 读取或保存审核意见 |
| POST | `/api/submissions/{id}/review/versions` | 创建反馈版本 |
| GET | `/api/submissions/{id}/review/versions/{versionId}/diff` | 对比反馈版本 |
| POST | `/api/submissions/{id}/review/complete` | 完成审核 |
| POST | `/api/submissions/{id}/issues` | 保存教师确认的问题 |
| GET/PUT | `/api/assignments/{id}/lesson-plan` | 读取或保存备课稿 |
| POST | `/api/assignments/{id}/lesson-plan/regenerate` | 重新生成备课稿 |
| GET | `/teacher/assignments/{id}/lesson-plan/export.doc` | 导出 Word 兼容文档 |
| GET | `/teacher/assignments/{id}/lesson-plan/export.md` | 导出 Markdown |

## 代码约定

- 采用 Controller、Service、Repository、Domain 分层。
- Controller 只负责参数接收、权限边界调用和响应编排。
- 业务规则集中在 Service，数据访问集中在 Repository。
- 公共类型和复杂规则使用 Javadoc 说明职责与设计原因。
- DTO 与实体分离，避免直接暴露持久化结构。
- HTML、CSS、JavaScript 和 Java 使用 UTF-8 编码。
- Java 使用 4 空格，HTML、CSS、JavaScript、YAML 使用 2 空格。
- 密码通过 BCrypt 哈希保存，不记录明文。
- 登录会话使用 Spring Security，写操作启用 CSRF 校验。
- 教师与学生按角色和课堂归属进行双层数据隔离。

## 已知边界

- 本地开发使用 H2，正式部署使用 Docker Compose 中的 PostgreSQL。
- 课堂讲解建议由确定性规则和教师确认记录生成，不替代教师判断。
- 临时公网隧道依赖第三方服务，不能作为长期生产部署方案。
- 演示账号密码和本地化工具路径需要在正式发布前调整。

## 团队信息

| 角色 | 姓名 | 学号/单位 | 项目分工 |
| --- | --- | --- | --- |
| 队员一 | （待填写） | （待填写） | 后端架构、账号权限、数据模型与课堂数据隔离 |
| 队员二 | （待填写） | （待填写） | 教师端、审核与备课界面，视觉和响应式 |
| 队员三 | （待填写） | （待填写） | 学生端、测试、依赖配置、部署和演示材料 |
| 指导教师 | （待填写） | （待填写） | 教学场景、诚信边界和阶段成果审核 |

详细分工与验收标准见 [docs/TEAM.md](docs/TEAM.md)。

## 仓库

- GitHub：<https://github.com/myy74261/homework-review>
- 分支：`main`
