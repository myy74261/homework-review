# 明鉴 · ReviewDesk 技术文档

更新日期：2026-10-30

本文说明 ReviewDesk 的系统边界、技术架构、数据模型、关键业务流程、AI 预审一致性策略、安全设计、部署方式和验证方法。安装步骤和主要接口清单见 [README](../README.md)，生产环境操作步骤见 [DEPLOYMENT.md](DEPLOYMENT.md)。

## 1. 项目定位

ReviewDesk 面向教师作业审核和备课整理场景。教师创建课堂、发布作业并设置评分量规，学生在线提交文字和附件，教师审核后发布反馈，平台再根据教师确认的问题生成课堂讲解建议和备课素材。

系统遵循以下边界：

1. AI 只生成待教师核实的预审建议。
2. AI 不直接写入最终评分、最终反馈或诚信结论。
3. 教师采纳 AI 建议后，建议才转为教师确认问题。
4. 诚信线索只作为核验提示，不构成自动作弊判定。
5. 学生只在教师发布反馈后看到最终结果。

## 2. 用户与权限

| 角色 | 主要能力 | 关键限制 |
| --- | --- | --- |
| 课堂所有者 | 创建课堂、导入名单、发布作业、配置量规、审核、评分、发布反馈、回复申诉、生成备课稿 | 只能管理自己创建的课堂 |
| 课堂协作教师 | 查看课堂提交、维护独立审核草稿 | 不能发布最终反馈、填写最终分、处理诚信标记、回复申诉或修改课堂设置 |
| 学生 | 加入课堂、提交作业、查看已发布反馈、在允许时申诉 | 只能访问自己加入的课堂和自己的提交 |
| AI 模型服务 | 根据提交文字、声明和评分量规生成建议 | 不拥有业务决策权限，不直接修改审核结果 |

权限检查分为两层：

1. Spring Security 按 `TEACHER` 和 `STUDENT` 角色限制入口。
2. `AccessControlService` 继续检查课堂归属、协作者关系、作业和提交归属。

## 3. 技术架构

```text
Browser
  React 19 + TypeScript + Vite
  TanStack Query + Zustand
          |
          | HTTPS / JSON / Multipart
          v
Nginx
          |
          v
Spring Boot 3.3 / Spring MVC / Tomcat
  Spring Security + CSRF + JDBC Session
  Teacher API / Student API / File API
          |
          v
Service Layer
  Account and Classroom
  Assignment and Submission
  Review and Version
  AI Batch Scheduler and AI Review
  Integrity and Appeal
  Lesson Plan
          |
          +-------------------+
          |                   |
          v                   v
Spring Data JPA        Local File Storage
  H2 / PostgreSQL        submission attachments
          |
          v
PostgreSQL
  business tables
  Flyway history
  Spring Session tables
```

外部 AI 服务通过 OpenAI 兼容 HTTP 接口接入。业务数据和模型建议保存在数据库中，提交附件保存在独立文件目录中。

### 3.1 技术栈

| 层次 | 技术 | 用途 |
| --- | --- | --- |
| 前端 | React 19、TypeScript、Vite | 教师端和学生端单页应用 |
| 前端组件 | Tailwind CSS、Radix/shadcn 基础组件 | 页面布局和交互 |
| 前端状态 | TanStack Query、Zustand | 服务端数据缓存与界面状态 |
| Web 层 | Spring MVC、Thymeleaf | JSON API、登录页和兼容页面 |
| 安全 | Spring Security、BCrypt、CSRF | 登录、密码哈希、角色和写操作保护 |
| 会话 | Spring Session JDBC | 生产环境跨重启保留登录会话 |
| 数据访问 | Spring Data JPA | 实体与数据库访问 |
| 数据库 | H2、PostgreSQL | 本地开发和正式部署 |
| 版本迁移 | Flyway | PostgreSQL 结构版本化 |
| AI 接入 | OpenAI 兼容 API | DeepSeek、OpenAI、通义千问、智谱、Moonshot 等 |
| 构建与测试 | Maven、npm、JUnit 5、MockMvc、Playwright | 构建、单元测试、接口测试和端到端测试 |

### 3.2 后端模块

| 模块 | 职责 |
| --- | --- |
| `domain` | 账号、课堂、作业、提交、审核、AI、申诉和备课实体 |
| `repository` | JPA 仓储接口和查询定义 |
| `service` | 业务规则、权限边界、审核版本、AI 批处理和建议生成 |
| `security` | Spring Security 配置和用户认证 |
| `bootstrap` | 首次启动时创建演示账号和示例数据 |
| `web` | REST 控制器、表单、DTO、文件下载和页面路由 |
| `web/spa` | React 教师端和学生端使用的 JSON API |

### 3.3 前端加载与拆包

前端入口只加载会话检查、应用外壳和共享依赖，教师端与学生端页面使用 `React.lazy` 按路由加载。这样，教师访问工作台时不会下载学生提交页，学生也不会下载备课和 AI 设置页面。

构建配置将依赖拆为以下共享包：

| Chunk | 内容 |
| --- | --- |
| `react-core` | React、React DOM、React Router |
| `query-core` | TanStack Query |
| `state-core` | Zustand |
| `ui-core` | Radix Slot、样式工具和表单基础组件 |
| `icons` | 实际使用的 Lucide 图标 |
| `dialog-core` | Radix Dialog，仅在打开对话框的页面加载 |

SheetJS `xlsx` 通过动态 `import()` 加载，只在教师导入 XLSX/XLS 名单时下载。当前首屏脚本约 364 KB，原先单入口约 495 KB，减少约 26.6%；各页面按需包约 4.6–32 KB。

## 4. 核心业务流程

```text
教师创建课堂
  -> 导入名单或发布课堂码
  -> 发布作业、截止时间、提交形式和评分量规
  -> 学生提交文字和附件
  -> 作业截止
  -> 自动创建 AI 预审任务
  -> 教师查看、采纳或忽略 AI 建议
  -> 教师确认问题、填写评分并发布反馈
  -> 学生查看结果，必要时申诉
  -> 教师汇总课堂问题
  -> 生成并编辑备课稿
  -> 导出 DOC 或 Markdown
```

AI 预审是教师审核前的辅助步骤。教师未配置模型时，系统不创建新的 AI 任务；已有审核流程不受影响。

## 5. 数据模型

系统采用逻辑外键和聚合服务管理数据，不依赖 JPA 双向关联表达全部业务关系。

| 数据组 | 主要表 | 说明 |
| --- | --- | --- |
| 账号 | `accounts` | 教师和学生共用账号表，通过角色区分 |
| 课堂 | `classrooms`、`classroom_memberships`、`classroom_collaborators`、`classroom_roster_entries` | 课堂、学生成员、协作教师和待关联名单 |
| 作业 | `assignments`、`assignment_rubrics` | 作业要求、截止时间、提交形式、AI 政策和评分量规 |
| 提交 | `submissions`、`submission_attachments` | 学生提交正文、AI 与来源声明、附件元数据 |
| 审核 | `review_drafts`、`collaborator_review_drafts`、`review_issues`、`review_scores`、`review_versions`、`review_events` | 所有者草稿、协作者草稿、确认问题、评分、版本和活动记录 |
| AI | `ai_provider_settings`、`ai_review_runs`、`ai_review_suggestions`、`ai_batch_review_jobs` | 教师模型配置、运行记录、建议和批量任务 |
| 诚信与申诉 | `integrity_flags`、`appeals` | 待核实线索、教师结论和学生申诉 |
| 备课 | `lesson_plans` | 课堂讲解建议和版本控制 |
| 会话 | Spring Session 表 | 持久化登录会话、属性和过期清理 |

### 5.1 数据归属

1. 课堂通过 `teacher_id` 关联所有者。
2. 学生通过 `classroom_memberships` 关联课堂。
3. 协作者通过 `classroom_collaborators` 单独关联课堂，每个协作者使用自己的教师账号。
4. 作业通过 `classroom_id` 关联课堂，提交通过 `assignment_id` 关联作业。
5. 附件文件使用 UUID 文件名保存在 `UPLOAD_ROOT`，数据库保存原始文件名、类型、大小和权限查询所需的提交编号。

## 6. 作业与截止时间

作业有两种主要状态：

| 状态 | 含义 | 学生可见 |
| --- | --- | --- |
| `PUBLISHED` | 已发布 | 是 |
| `WITHDRAWN` | 已撤回，可修改后重新发布 | 否 |

已发布作业可以直接修改截止时间，新时间必须晚于当前时间。撤回后重新发布也可以更换截止时间，已有提交记录继续保留。

截止时间变化会同时影响 AI 自动预审：

1. 未截止作业不会创建新的 AI 任务。
2. 已存在但尚未执行的待处理任务，会同步到新的截止时间。
3. 已领取但尚未调用模型的任务，会在模型调用前再次检查截止条件；条件不成立时放回待处理状态。
4. 已完成、失败或跳过的运行记录不因截止时间变化而删除或重置。
5. 教师最终评分、反馈、申诉和审核记录不参与任务同步，不会被覆盖。

## 7. AI 自动预审

### 7.1 设计原则

AI 预审采用异步任务方式，避免模型请求阻塞学生提交和教师页面。任务只保存模型建议，最终结果仍由教师决定。

默认运行参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_BATCH_SIZE` | `3` | 每轮最多处理的任务数 |
| `AI_BATCH_INITIAL_DELAY_MS` | `15000` | 服务启动后首次扫描等待时间 |
| `AI_BATCH_DELAY_MS` | `60000` | 两轮扫描之间的固定等待时间 |
| 最大尝试次数 | `3` | 固定业务规则 |
| 失败重试间隔 | `5` 分钟 | 固定业务规则 |

### 7.2 调度流程

```text
Scheduler
  -> 查询当前截止时间已到的已发布作业
  -> 检查课堂未解散、教师已配置 AI
  -> 为未审核且没有任务的提交创建 PENDING 任务
  -> 领取 next_attempt_at 已到的任务
  -> 再次检查作业发布状态和当前截止时间
  -> 调用模型
  -> 保存运行记录和建议
  -> 将任务标记为 COMPLETED
```

新任务依据提交编号保证幂等。`ai_batch_review_jobs.submission_id` 有唯一约束，仓储层在创建前也会检查是否已有任务。

### 7.3 任务状态

| 状态 | 含义 | 后续处理 |
| --- | --- | --- |
| `PENDING` | 等待执行或等待重试 | 到达 `next_attempt_at` 后可领取 |
| `RUNNING` | 已被调度器领取 | 执行模型调用或在校验失败后放回队列 |
| `COMPLETED` | 模型调用成功并保存建议 | 保留运行历史 |
| `FAILED` | 达到最大尝试次数 | 保留失败原因，不删除历史 |
| `SKIPPED` | 提交已删除或教师已完成审核 | 保留跳过原因 |

### 7.4 截止时间一致性

截止时间变更涉及以下主要场景：

| 场景 | 系统处理 |
| --- | --- |
| 截止前延长截止时间，尚无任务 | 不创建任务，等待新截止时间 |
| 截止后延长截止时间，任务仍为 `PENDING` | 将任务执行时间同步为新截止时间 |
| 调度器刚领取任务，教师同时延长截止时间 | 模型调用前重新读取作业，放回 `PENDING`，撤销本次尝试计数 |
| 任务已 `COMPLETED` | 保持状态、尝试次数和执行时间不变 |
| 作业撤回 | 不创建新任务；已领取任务在模型调用前重新校验 |
| 课堂解散 | 不创建新任务；已有任务和运行记录保留 |

该策略解决了两类不一致：

1. 提交记录可能保存旧截止时间，任务创建不能只依赖提交时的快照。
2. 教师修改截止时间和调度器领取任务可能同时发生，仅同步数据库中的 `PENDING` 任务仍存在竞争窗口。

### 7.5 模型输入与输出

模型输入包括：

- 作业标题和提交形式。
- 学生提交正文。
- AI 使用情况、工具、范围、文献来源和协作声明。
- 作业评分量规。

当前模型不解析附件二进制内容。PDF、Word、代码压缩包和截图保存在附件存储中，审核时由教师查看；如果后续需要理解附件内容，应增加文本提取或 OCR 环节，并重新评估隐私和失败处理。

模型输出保存为 `ai_review_runs` 和 `ai_review_suggestions`。教师可以：

1. 采纳建议，生成教师确认问题。
2. 忽略建议，保留处理记录。
3. 不采用 AI 建议，直接完成审核。

### 7.6 失败处理

| 情况 | 行为 |
| --- | --- |
| 教师未配置模型 | 不创建新任务；后续扫描时重新检查 |
| 模型返回无效格式 | 任务按失败处理 |
| 网络或接口异常 | 5 分钟后重试，最多尝试 3 次 |
| 提交已审核 | 任务标记为 `SKIPPED` |
| 作业已撤回 | 不调用模型，任务等待后续状态变化 |
| 课堂已解散 | 不创建新任务 |

## 8. 学生提交与附件

学生可以提交文字和最多 8 个附件，每个附件最大 25 MB，单次请求最大 220 MB。

支持的文件类别：

| 类别 | 扩展名 |
| --- | --- |
| 文档 | `pdf`、`doc`、`docx`、`ppt`、`pptx`、`xls`、`xlsx`、`csv`、`rtf`、`odt`、`ods`、`odp`、`txt`、`md` |
| 代码 | `java`、`py`、`js`、`ts`、`c`、`cpp`、`h`、`cs`、`go`、`rs`、`sql`、`json`、`xml`、`yml`、`yaml`、`ipynb`、`html`、`css` |
| 压缩包 | `zip`、`rar`、`7z` |
| 截图 | `png`、`jpg`、`jpeg`、`webp` |

文件下载请求需要先通过作业和课堂权限检查。系统只使用数据库中保存的 UUID 文件名访问存储目录，原始文件名仅作为下载展示。

## 9. 审核、诚信和备课

### 9.1 审核版本

课堂所有者和协作者使用不同草稿表保存审核内容，避免协作覆盖。最终反馈、最终分和确认问题的发布动作只允许课堂所有者执行。

反馈版本保存审核正文快照。版本对比使用行级差异计算，恢复操作会生成新的当前版本并保留历史。

### 9.2 诚信核验

诚信线索由教师创建或从 AI 建议中整理。线索具有待核实、确认或排除等处理状态。只有教师确认并向学生发布的结果才在学生端展示。

### 9.3 备课建议

平台按课堂汇总已确认问题，生成课堂目标、错例分析、追问和练习段落。备课稿支持：

- 手动编辑和自动保存。
- 根据当前确认问题重新生成。
- 版本冲突检测。
- 导出 Word 兼容 `.doc` 和 Markdown。

## 10. 安全设计

| 风险 | 当前措施 |
| --- | --- |
| 越权访问 | 角色路由和课堂归属双重检查 |
| 会话劫持 | 登录后迁移会话标识；生产 Cookie 使用 `Secure`、`HttpOnly` 和 `SameSite=Lax` |
| 会话丢失 | 生产使用 Spring Session JDBC 保存到 PostgreSQL |
| 密码泄露 | BCrypt 哈希保存，不返回密码字段 |
| CSRF | Spring Security 对写操作启用 CSRF 校验 |
| 附件路径穿越 | UUID 存储名、路径归一化、按提交编号查询 |
| AI Key 泄露 | AES-GCM 加密保存，页面只显示末四位 |
| 模型建议越权 | AI 建议独立存储，教师显式采纳后才转为审核问题 |
| 数据覆盖 | 审核版本、运行记录和任务历史独立保存 |

密钥、数据库密码和 SSH 私钥不得写入 Git、前端构建产物或日志。

## 11. 数据库与迁移

本地开发默认使用 H2 文件数据库，Hibernate 负责开发表结构。正式环境使用 PostgreSQL，并设置：

```dotenv
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
SESSION_STORE_TYPE=jdbc
```

当前 Flyway 版本：

| 版本 | 内容 | 状态 |
| --- | --- | --- |
| `V1` | 业务表基线 | 已执行 |
| `V2` | Spring Session JDBC 表 | 已执行 |

已执行的生产迁移不能修改。后续数据库结构变化新增 `V3`、`V4` 等迁移文件，并在部署前备份数据库和上传目录。

本次 AI 截止时间一致性修复不改变表结构，因此没有新增 Flyway 迁移。

## 12. 部署与运行

系统支持两种部署方式：

| 方式 | 组成 | 适用情况 |
| --- | --- | --- |
| Docker Compose | Spring Boot、PostgreSQL、Nginx | 可访问 Docker Hub，希望容器隔离 |
| 原生 Ubuntu | JDK 17、systemd、PostgreSQL、Nginx | Docker Hub 受限或希望减少运行层级 |

当前阿里云 ECS 使用原生部署：

- Ubuntu 22.04
- JDK 17
- PostgreSQL 14
- Nginx
- systemd 服务 `review-desk`
- HTTPS 地址：`https://47-114-60-5.sslip.io/login`

生产更新流程：

1. 运行后端测试并构建 JAR。
2. 记录本地 JAR 的 SHA-256。
3. 上传到服务器临时文件并核对 SHA-256。
4. 备份当前 `/opt/review-desk/app.jar`。
5. 替换 JAR，保留 `reviewdesk` 用户和文件权限。
6. 重启 `review-desk` 服务。
7. 检查服务状态、Flyway 日志和登录页 HTTPS 响应。
8. 确认线上 JAR 哈希与本地构建一致。

详细命令见 [DEPLOYMENT.md](DEPLOYMENT.md)。

## 13. 可观测性与排查

当前可观测信息主要来自：

- systemd 服务状态和 `journalctl` 日志。
- Flyway 迁移校验结果。
- Hibernate 启动校验结果。
- AI 批量任务状态和失败原因。
- AI 运行记录、建议状态和审核活动记录。

常用检查：

```bash
systemctl is-active review-desk
journalctl -u review-desk -n 100 --no-pager
curl -I https://47-114-60-5.sslip.io/login
```

排查 AI 任务时先检查：

1. 教师是否在个人中心配置了可用的 AI 接口。
2. 作业是否为 `PUBLISHED`。
3. 当前截止时间是否已经到达。
4. 课堂是否已解散。
5. 提交是否已经完成审核。
6. 任务的 `next_attempt_at`、尝试次数和失败原因。

## 14. 测试与验证

后端使用 Spring Boot Test、JUnit 5 和 MockMvc，测试数据使用独立 H2 内存数据库。前端使用 TypeScript、ESLint、Prettier 和 Playwright。

当前回归覆盖包括：

- 账号注册、登录、角色隔离和课堂数据隔离。
- 课堂码、名单导入和协作教师权限。
- 动态评分量规、混合提交形式和附件访问。
- 审核草稿、版本对比、评分、诚信和申诉。
- 问题汇总、备课生成和文档导出。
- AI 自动任务创建、幂等和模型失败处理。
- 截止时间延长后任务推迟。
- 已领取任务在模型调用前放回队列。
- 截止时间变化后已完成运行记录保持不变。

截至 2026-10-30，后端自动化测试共 51 项通过。实际验证时以当前分支的测试结果为准：

```powershell
$env:JAVA_HOME = "D:\Project\jdk"
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" test
```

## 15. 已知边界与后续方向

1. AI 当前不解析附件内容，只处理提交正文和声明。
2. AI 未配置或接口不可用时，不生成新的模型建议，教师仍可完成规则审核。
3. 协作者不能执行最终发布和申诉回复，相关权限集中在课堂所有者。
4. 附件使用本地文件系统，扩容或迁移时应同步备份 `UPLOAD_ROOT`。
5. 当前未引入消息队列，AI 调度由应用内定时任务完成；单实例部署满足现有规模。
6. 如果后续改为多实例部署，应重新评估任务领取锁、调度重复执行和文件存储共享。
7. 课堂解散后不会创建新的 AI 任务；已有任务当前没有单独的撤销状态。

## 16. 相关文档

- [README](../README.md)：项目介绍、功能、启动和接口概览。
- [ENVIRONMENT.md](ENVIRONMENT.md)：环境变量和本地、测试配置。
- [DEPENDENCIES.md](DEPENDENCIES.md)：依赖清单与升级说明。
- [DEPLOYMENT.md](DEPLOYMENT.md)：Docker 与原生 Ubuntu 部署。
- [TEAM.md](TEAM.md)：团队分工与验收范围。
