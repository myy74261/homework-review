# 明鉴 · ReviewDesk

面向“智慧教育”场景的教师备课辅助与作业审核平台。平台连接教师发布、学生提交、问题确认、反馈版本和后续讲解建议，帮助教师节省重复整理时间，同时保留证据来源与教师最终判断。

## 作业目标

- 不做 AI 判定，不替教师作最终评价。
- 在审核过程中定位证据、方法、引用和表达上的薄弱项。
- 让教师反馈包含具体位置、判断依据和下一步修改动作。
- 保留完整审核轨迹，支持教师复盘与课程分析。

## 当前阶段

P3：基于教师确认问题生成可编辑、可追溯的课堂讲解建议。

本阶段完成：

- 确定性规则根据评分维度生成课堂目标、共性问题、典型错例、追问和练习
- 教师可编辑并自动保存备课稿，重新生成时形成新版本
- 备课页并列保留学生原文、教师判断和对应审核入口
- 平台建议明确标注判断边界，不进行自动作弊判定
- 教师发布作业并配置四项评分量规
- 学生端查看要求和评分依据、在线提交与重新提交
- 教师课堂首页查看作业、提交队列与共性问题
- 教师确认审核提示后，按评分维度写入共性问题
- 审核过程事件采集，包括打开提交、保存反馈、创建版本和采纳提示
- 教师反馈保存到 H2，刷新页面不丢失
- 停止输入 900 毫秒后自动保存，失败时退回本机审核草稿
- 保存时检测版本冲突，避免静默覆盖
- 手动创建反馈版本，查看时间、字数与触发方式
- 反馈版本逐行对比、历史反馈恢复
- PC 三栏、平板双栏和手机单栏布局保持完整

> 审核页右侧仍是量规提示卡片；备课建议已经使用真实问题数据通过规则生成。P4 将完成导出、归档与课程看板。

## 阶段路线

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 独立工程与响应式教师审核工作台 | 已完成 |
| P1 | 审核意见自动保存、反馈版本与对比 | 已完成 |
| P2 | 作业发布、学生提交、审核事件与量规问题汇总 | 已完成 |
| P3 | 可解释备课建议生成与教师编辑 | 已完成 |
| P4 | 审核档案、课程与教师看板 | 待开始 |

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 运行环境 | Java 17 |
| 后端 | Spring Boot 3.3、Spring MVC、Thymeleaf |
| 数据 | Spring Data JPA、H2 文件数据库 |
| 前端 | 原生 HTML / CSS / JavaScript |
| 构建 | Maven 3.10 |

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/submissions/{id}/review` | 读取当前审核意见 |
| PUT | `/api/submissions/{id}/review` | 保存审核意见，使用 `baseRevision` 防止覆盖 |
| POST | `/api/submissions/{id}/review/versions` | 创建反馈版本 |
| GET | `/api/submissions/{id}/review/versions` | 审核记录列表 |
| GET | `/api/submissions/{id}/review/versions/{versionId}` | 审核记录详情 |
| GET | `/api/submissions/{id}/review/versions/{versionId}/diff` | 与当前反馈逐行对比 |
| POST | `/api/submissions/{id}/review/versions/{versionId}/restore` | 恢复历史反馈 |
| POST | `/api/submissions/{id}/issues` | 保存教师确认的量规问题 |
| POST | `/api/submissions/{id}/events` | 记录关键审核事件 |
| GET | `/api/assignments/{id}/lesson-plan` | 获取备课建议与问题依据 |
| PUT | `/api/assignments/{id}/lesson-plan` | 自动保存教师编辑的备课稿 |
| POST | `/api/assignments/{id}/lesson-plan/regenerate` | 按最新问题重新生成备课稿 |

## 演示流程

1. 教师进入 `/teacher`，发布作业并设置评分量规。
2. 学生进入 `/student`，查看作业要求并提交内容。
3. 教师从提交队列进入审核工作台，确认问题并生成反馈。
4. 教师首页按评分维度查看共性问题汇总。
5. 教师打开备课建议，核对问题依据、编辑课堂讲解并按需重新生成。

## 本地运行

本机工具路径：

- JDK：`D:\Project\jdk`
- Maven：`D:\Project\apache-maven-3.10.0`

```powershell
cd D:\workspace\Web\review-desk
$env:JAVA_HOME = "D:\Project\jdk"
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

浏览器访问 <http://localhost:8080>。

## 代码约定

- Controller 只负责请求编排，业务流程集中在 service。
- 公共类型与复杂规则写 Javadoc，说明设计原因。
- 配置与密钥不进入仓库。
- 每个阶段单独提交，保持作业过程可追踪。

## 代码结构

```text
src/main/java/com/reviewdesk
├── ReviewDeskApplication.java     应用入口
├── domain                         作业、量规、提交、审核、问题、事件与备课稿
├── repository                     JPA 数据访问
├── service                        审核版本、问题汇总与规则式备课建议
├── bootstrap                      教师审核演示数据
└── web                            页面路由、REST 接口与 DTO

src/main/resources
├── application.yml                应用配置
├── templates                      教师端、学生端与审核工作台
└── static
    ├── css                        教师端、学生端与审核样式
    └── js                         审核交互与本机草稿
```

