# 依赖清单

## 1. 构建与运行环境

| 依赖 | 版本/要求 | 用途 |
| --- | --- | --- |
| Java | 17 | 编译和运行 |
| Maven | 3.10 | 依赖管理、测试和打包 |
| H2 | 2.2.224 | 本地文件数据库 |
| 浏览器 | 现代版本 | 访问教师端与学生端 |

前端使用原生 HTML、CSS 和 JavaScript，不依赖 Node.js、npm 或前端打包工具。

## 2. Maven 直接依赖

版本由 `spring-boot-starter-parent:3.3.5` 统一管理。

| 依赖 | 作用域 | 作用 |
| --- | --- | --- |
| `spring-boot-starter-web` | runtime | Spring MVC、嵌入式 Tomcat、Jackson |
| `spring-boot-starter-thymeleaf` | runtime | 服务端 HTML 模板渲染 |
| `spring-boot-starter-data-jpa` | runtime | JPA、Hibernate、事务和仓储 |
| `spring-boot-starter-validation` | runtime | Jakarta Validation 与表单校验 |
| `spring-boot-starter-security` | runtime | 登录会话、角色权限、CSRF |
| `com.h2database:h2` | runtime | H2 文件数据库驱动 |
| `spring-boot-starter-test` | test | JUnit 5、AssertJ、MockMvc |
| `spring-security-test` | test | 模拟教师/学生登录与 CSRF |

## 3. 主要传递依赖

| 组件 | 当前版本 | 来源 |
| --- | --- | --- |
| Spring Boot | 3.3.5 | 父工程 |
| Spring Framework / MVC | 6.1.x | Spring Boot |
| Spring Security | 6.3.4 | Spring Boot |
| Hibernate ORM | 6.5.3.Final | Spring Data JPA |
| H2 Database | 2.2.224 | Boot 依赖管理 |
| Tomcat Embedded | 10.1.31 | Web Starter |
| Jackson | 2.x | Web Starter |
| JUnit Jupiter | 5.10.5 | Test Starter |

实际版本以下面的 Maven 输出为准：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" dependency:tree
```

仅查看运行时依赖：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" dependency:tree -Dscope=runtime
```

## 4. Maven 插件

| 插件 | 作用 |
| --- | --- |
| `maven-compiler-plugin` | Java 17 编译 |
| `maven-surefire-plugin` | JUnit 5 测试执行 |
| `spring-boot-maven-plugin` | 生成可执行 Spring Boot JAR |

## 5. 更新依赖

升级 Spring Boot 时修改 `pom.xml` 中的父工程版本：

```xml
<version>3.3.5</version>
```

升级后必须执行：

```powershell
& "D:\Project\apache-maven-3.10.0\bin\mvn.cmd" clean test
```

不要在未运行测试的情况下直接升级安全组件、JPA 或数据库驱动。

