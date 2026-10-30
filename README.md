# 明鉴 · ReviewDesk

面向「智慧教育」场景的教师备课辅助与作业审核平台。项目连接教师发布作业、学生提交、教师审核、共性问题汇总、课堂讲解建议和备课素材导出，帮助教师减少重复整理工作，同时保留证据来源、审核记录和教师最终判断。

项目不进行 AI 作弊判定，不以模型概率判断学生是否独立完成作业。平台只整理教师确认的问题，所有课堂建议均由教师审核后使用。

## 项目目标

- 缩短教师从批阅作业到整理课堂共性问题的时间。
- 让教学反馈对应学生原文、评分量规和教师判断。
- 保留完整审核轨迹，支持版本对比与历史恢复。
- 明确区分学生原文、平台建议和教师最终意见。
- 支持从教师发布、学生提交、教师审核到备课素材导出的完整教学流程。

## 核心流程

```text
教师创建课堂并设置课程时间
  → 导入学生名单或使用课堂码加入
  → 按需邀请课堂协作管理员
  → 教师发布作业和评分量规
  → 学生在线提交
  → 作业截止后自动创建 AI 预审建议
  → 所有者与协作者分别保存独立审核草稿
  → 所有者汇总意见、确认问题并发布最终反馈
  → 按课堂汇总共性问题与提交占比
  → 生成可编辑的课堂讲解建议
  → 导出备课材料
  → 课程结束后选择是否解散课堂
```

## 功能概览

### 教师端

- 注册、登录、切换账号、退出和安全注销，只能访问自己有权管理的课堂数据。
- 在个人中心维护姓名、邮箱、院系、职称，设置显示偏好并修改密码。
- 创建课堂并生成唯一课堂码，设置课程起止时间；结课后可软解散课堂。
- 启用课堂协作管理员，邀请独立教师账号参与审核，并随时移除协作者。
- 使用 XLSX、XLS、CSV、TXT、粘贴表格或课堂码建立学生名单。
- 查看每个课堂独立的提交学生占比和问题维度分布。
- 发布作业，选择文档、代码、截图/设计或混合提交形式。
- 已发布作业可直接调整截止时间，无需撤回整份作业。
- 配置 1–10 项总分 100 分的动态评分量规。
- 设置 AI 使用政策，并决定是否要求学生声明 AI、文献来源和协作情况。
- 查看提交队列、审核状态和共性问题。
- 查看学生上传的文档、源代码、压缩包或截图，并按附件权限安全下载。
- 根据作业形式显示精简后的审核重点，减少重复检查。
- 自动保存审核意见，检测版本冲突。
- 协作者拥有互不覆盖的独立审核草稿，所有者可查看汇总后发布最终意见。
- 创建反馈版本，逐行对比并恢复历史反馈。
- 确认学生问题，保留原文证据和教师修改建议。
- 调用可选 AI 模型生成逐条预审建议，保存模型、时间和处理状态。
- 作业截止后自动扫描班级提交并启动批量 AI 预审；调整或重新发布截止时间会同步待执行任务，无需教师手动开始。
- 教师逐条采纳或忽略 AI 建议；采纳后才转为教师确认问题并写入反馈草稿。
- 根据已确认问题生成可解释的规则评分建议，教师可独立填写最终分。
- 记录待核实的诚信线索，只有教师确认后才向学生展示。
- 处理学生申诉，保留复核结论和教师说明。
- 根据共性问题生成课堂目标、错例分析、追问和练习。
- 编辑并自动保存备课稿，支持重新生成。
- 导出 Word 兼容 `.doc` 和 Markdown 文档。
- 完成审核后向学生发布最终反馈。

### 学生端

- 注册、登录和退出。
- 使用教师提供的课堂码加入；课堂启用名单后仅名单内学生可加入。
- 已注册名单学生自动关联课堂，未注册学生注册后自动关联。
- 查看教师发布的作业要求和评分量规。
- 在线提交文字，并上传文档、代码、截图或压缩包。
- 声明 AI 使用情况、工具、使用范围、文献与数据来源、协作情况。
- 每次提交保留为独立记录。
- 审核完成后查看教师最终反馈、各维度评分和已确认的诚信核验结果。
- 在教师开放申诉时提交异议，并查看复核状态与处理说明。

## 技术架构

```text
浏览器
├── React 19 + TypeScript + Vite
├── Tailwind CSS + shadcn/Radix 基础组件
├── TanStack Query 服务端状态
└── Zustand 会话提示与界面状态
        │
Spring MVC
├── 教师端、学生端及审核控制器
├── React JSON API 与 OpenAI 兼容模型适配层
├── Spring Security + Spring Session JDBC 会话与角色权限
└── 全局异常处理
        │
业务服务层
├── 账号与课堂
├── 作业发布、混合附件与 AI 政策
├── 审核意见与历史版本
├── 可解释评分、诚信核验与申诉
├── 问题汇总与备课建议
├── AI 预审任务调度、重试与截止时间校验
└── 导出与访问边界校验
        │
Spring Data JPA
├── H2 文件数据库（本地开发）
└── PostgreSQL（生产部署）
```

| 模块 | 职责 |
| --- | --- |
| `domain` | 账号、课堂、作业、提交、附件、评分、诚信核验、申诉、审核与备课实体 |
| `repository` | JPA 仓储接口，集中声明数据访问方法 |
| `service` | 业务规则、权限边界、版本控制、问题汇总与建议生成 |
| `security` | Spring Security 用户加载、会话和角色路由 |
| `web` | 页面路由、表单、REST 控制器与 DTO |
| `bootstrap` | 首次启动时的演示账号、课堂和作业数据 |
| `templates` | 登录、教师端、学生端、审核和备课页面 |
| `static` | 响应式 CSS、自动保存和页面交互脚本 |

完整架构、数据模型、AI 任务状态和一致性设计见
[docs/TECHNICAL_DOCUMENT.md](docs/TECHNICAL_DOCUMENT.md)。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 运行环境 | Java 17 |
| 后端 | Spring Boot 3.3.5、Spring MVC、Thymeleaf |
| 安全 | Spring Security 6.3、BCrypt、CSRF、角色权限 |
| 会话 | Spring Session JDBC + PostgreSQL 持久化 |
| 数据 | Spring Data JPA、Flyway、H2（开发）、PostgreSQL（部署） |
| 前端 | React 19、TypeScript、Vite、Tailwind CSS、Radix/shadcn 基础组件 |
| 前端状态 | TanStack Query、Zustand |
| 表格解析 | SheetJS `xlsx`（按需加载） |
| AI 接入 | 可选 OpenAI 兼容 API（默认示例为 DeepSeek） |
| 构建 | Maven 3.10、Node.js 22、npm 10 |
| 测试与规范 | JUnit 5、MockMvc、TypeScript、ESLint、Prettier |

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
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── src/test/java/com/reviewdesk/web
├── docs/
│   ├── ENVIRONMENT.md             完整环境变量与配置说明
│   ├── DEPENDENCIES.md            依赖清单与升级方式
│   ├── DEPLOYMENT.md              Docker 与原生 Ubuntu 部署说明
│   ├── TECHNICAL_DOCUMENT.md      系统架构、数据模型与关键流程
│   └── TEAM.md                    团队职责与验收范围
├── .env.example                   环境变量模板
├── .env.production.example        生产环境变量模板
├── Dockerfile                     多阶段 Java 构建镜像
├── docker-compose.yml             应用与 PostgreSQL 编排
├── deploy/nginx-review-desk.conf  Nginx 反向代理模板
├── deploy/native.env.example     原生服务器环境模板
├── deploy/setup-native-db.sh     PostgreSQL 初始化脚本
├── deploy/review-desk.service    systemd 服务配置
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
- Node.js 22 与 npm（仅修改前端时需要）
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

### 构建 React 前端

```powershell
cd D:\workspace\Web\review-desk
.\build-frontend.cmd
```

生产构建输出到 `src/main/resources/static/app/`，随后由 Maven 打进 JAR。
登录页继续使用原 Thymeleaf 页面，教师端和学生端由 React 单页应用提供。

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

项目支持 Docker Compose 和原生 JDK + PostgreSQL 两种部署方式。

当前阿里云 ECS 使用原生 Ubuntu、JDK 17、PostgreSQL、Nginx 和 systemd，HTTPS 地址为：

```text
https://47-114-60-5.sslip.io/login
```

原来的 HTTP IP 地址会通过 Nginx 自动跳转到 HTTPS。生产配置模板见
`deploy/nginx-review-desk-https.conf`，证书由 Certbot 自动续期。

Docker 服务器首次部署：

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

首次启动时会自动创建演示账号，并建立「传播学方法课 · 2026 秋」课堂、学生成员关系和示例作业。

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

学生提交附件默认保存在 `data/uploads/`。生产环境通过 `UPLOAD_ROOT`
指定独立目录；数据库备份和上传目录需要同时备份。

应用配置位于：

```text
src/main/resources/application.yml
```

### 可选 AI 模型配置

项目默认使用可解释的确定性规则。教师登录后可在顶部「AI 设置」中选择
DeepSeek、OpenAI、通义千问、智谱或 Moonshot，并填写自己的模型密钥。
学生端不显示该配置入口，服务端也会拒绝学生访问教师 AI 配置接口。

教师 API Key 使用 AES-GCM 加密后保存，页面只显示末四位。服务端也可以通过环境变量提供默认
OpenAI 兼容接口：

```dotenv
AI_API_KEY=服务端模型密钥
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat
AI_TIMEOUT_SECONDS=30
AI_SETTINGS_SECRET=用于加密教师 API Key 的长随机字符串
```

密钥不得写入前端代码或提交到 Git。模型失败时系统自动回退到规则稿。
AI 预审建议不会直接修改评分、最终反馈或诚信结论，只有教师主动采纳后才会进入教师问题记录。

自动预审任务默认每 60 秒扫描一次，每轮最多处理 3 份提交；失败任务最多重试 3 次。
可通过 `AI_BATCH_SIZE`、`AI_BATCH_INITIAL_DELAY_MS` 和 `AI_BATCH_DELAY_MS` 调整。
AI 自动预审依据作业当前截止时间；调整或重新发布截止时间会同步待执行任务，已完成建议和运行记录保留。
模型调用前还会再次检查作业状态和截止时间，避免调度器领取任务与教师延长截止时间同时发生时提前调用 AI。

## 测试与构建

运行全部测试：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" test
```

React 页面端到端测试（需要本机 Edge 和已打包 JAR）：

```powershell
cd frontend
npm run test:e2e
```

跳过测试打包：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" -DskipTests package
```

测试覆盖账号注册登录、角色隔离、课堂码与名单加入、课堂协作者权限、动态量规、
混合附件提交、AI 与来源声明校验、审核保存、版本对比与恢复、可解释评分、
诚信核验可见性、学生申诉、问题汇总、备课生成编辑、文档导出，以及 AI 批处理与
截止时间变更的一致性。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/login` | 账号登录 |
| POST | `/register` | 注册教师或学生账号 |
| POST | `/teacher/classrooms` | 创建课堂 |
| POST | `/api/app/teacher/classrooms/{id}/roster` | 导入学生名单 |
| POST | `/api/app/teacher/classrooms/{id}/collaborators` | 邀请课堂协作者 |
| PUT | `/api/app/teacher/classrooms/{id}/collaboration` | 启用或关闭课堂协作 |
| PUT | `/api/app/teacher/assignments/{id}/due-date` | 直接调整已发布作业截止时间 |
| POST | `/student/join` | 使用课堂码加入课堂 |
| POST | `/teacher/assignments` | 发布作业 |
| POST | `/student/assignments/{id}/submissions` | 提交作业 |
| GET | `/files/submissions/{id}/attachments/{attachmentId}` | 权限校验后读取附件 |
| POST | `/teacher/submissions/{id}/scores` | 保存各维度最终评分 |
| POST | `/teacher/submissions/{id}/integrity-flags` | 记录待核实诚信线索 |
| POST | `/teacher/integrity-flags/{id}/resolve` | 确认或排除诚信线索 |
| GET/PUT | `/api/submissions/{id}/review` | 读取或保存审核意见 |
| POST | `/api/submissions/{id}/review/versions` | 创建反馈版本 |
| GET | `/api/submissions/{id}/review/versions/{versionId}/diff` | 对比反馈版本 |
| POST | `/api/submissions/{id}/review/complete` | 完成审核 |
| POST | `/api/submissions/{id}/issues` | 保存教师确认的问题 |
| POST | `/student/submissions/{id}/appeals` | 学生提交申诉 |
| POST | `/teacher/appeals/{id}/respond` | 教师回复申诉 |
| PUT | `/api/app/account/password` | 教师或学生校验旧密码后修改密码 |
| GET/PUT | `/api/app/teacher/ai-settings` | 教师读取或加密保存 AI 接口配置 |
| POST | `/api/app/teacher/ai-settings/test` | 教师测试模型连接 |
| POST | `/api/app/teacher/reviews/{id}/ai-review` | 生成并留痕 AI 预审建议 |
| POST | `/api/app/teacher/ai-review/suggestions/{id}/adopt` | 教师采纳 AI 建议 |
| POST | `/api/app/teacher/ai-review/suggestions/{id}/dismiss` | 教师忽略 AI 建议 |
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
- 生产登录会话保存到 PostgreSQL，应用重启后仍保持登录。
- 生产数据库由 Flyway 版本化迁移，Hibernate 只执行结构校验。
- 教师与学生按角色和课堂归属进行双层数据隔离。

## 已知边界

- 本地开发使用 H2，正式部署使用 Docker Compose 中的 PostgreSQL。
- 课堂讲解建议由确定性规则和教师确认记录生成，不替代教师判断。
- 评分建议始终由教师决定；AI 只生成待核实线索。AI 预审和备课增强必须配置模型密钥，调用失败时不会改变教师最终结果。
- AI 当前读取学生提交正文、AI 与来源声明和评分量规，不解析附件二进制内容；附件由教师打开查看。
- 诚信标记用于提示教师核验，不构成自动作弊判定；只有已确认结果向学生展示。
- 协作者可以参与审核并维护独立草稿，但不能发布最终反馈、填写最终分、处理诚信标记、回复申诉或修改课堂设置。
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
