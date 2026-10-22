# 明鉴 · ReviewDesk

面向高校教师的作业审核与反馈平台。平台把学生提交、评分量规、审核提示和反馈版本集中在同一工作台，帮助教师形成有依据、可执行、可追溯的审核意见。

## 作业目标

- 不做 AI 判定，不替教师作最终评价。
- 在审核过程中定位证据、方法、引用和表达上的薄弱项。
- 让教师反馈包含具体位置、判断依据和下一步修改动作。
- 保留完整审核轨迹，支持教师复盘与课程分析。

## 阶段路线

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 独立工程与响应式教师审核工作台 | 已完成 |
| P1 | 审核意见自动保存、反馈版本与对比 | 待开始 |
| P2 | 审核过程事件与量规指标 | 待开始 |
| P3 | 审核规则引擎与实时反馈卡片 | 待开始 |
| P4 | 审核档案、课程与教师看板 | 待开始 |

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 运行环境 | Java 17 |
| 后端 | Spring Boot 3.3、Spring MVC、Thymeleaf |
| 数据 | Spring Data JPA、H2 文件数据库 |
| 前端 | 原生 HTML / CSS / JavaScript |
| 构建 | Maven 3.10 |

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

