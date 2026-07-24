# AgentCache 项目规范

## 项目全景

AgentCache 是一个面向小团队的文件中转与交接服务，支持：

- 人通过 Web 管理面板管理文件和空间
- Agent 通过本地 CLI 上传、下载、分享文件
- 文件支持本地磁盘和腾讯云 COS 两种存储后端
- 文件可见性支持 PRIVATE（需权限）和 PUBLIC（公开访问）

详细设计文档见 `docs/` 目录。

其他未在本文件明确说明的编码规范，遵循通用 Java/Spring 与 Vue 社区约定。

---

## 技术栈

- **后端**：Java 17 / Spring Boot 3.3 / Spring Security / JPA / Flyway / MySQL 8.4 / Maven
- **前端**：Vue 3 / TypeScript / Pinia / Vue Router / Element Plus / Vite
- **CLI**：Java 17 / Picocli / Apache HttpClient 5 / Jackson / Maven
- **部署**：Docker + Docker Compose

---

## 项目结构

```
AgentCache/
├── backend/              # Spring Boot 单模块后端项目
│   └── server/           # 唯一模块，按包分层
│       └── src/main/java/com/agentcache/
│           ├── server/           # Web 层：启动、Controller、DTO、Security、异常处理
│           ├── application/      # 应用层：Service、用例编排、应用 DTO
│           ├── domain/           # 领域层：实体、枚举、Repository 接口、Port 接口
│           ├── infrastructure/   # 基础设施层：存储适配器等 Port 实现
│           └── common/           # 公共：通用异常、响应封装、工具类
├── frontend/             # Vue 3 前端项目
├── cli/                  # Java CLI 项目
├── docker/               # Docker 相关文件
├── docs/                 # 设计文档和实施计划
└── CLAUDE.md             # 本文件
```

**规范说明**：

- `frontend/` 和 `backend/` 为独立项目，分别使用 npm/yarn 和 Maven 管理依赖
- `backend/` 为 Maven 单模块（`server`），通过包结构表达分层，不再拆分物理模块
- 各层职责由包名划分（`server` / `application` / `domain` / `infrastructure` / `common`），新增代码按职责归入对应包

---

## 后端包结构与分层

后端为单 Maven 模块 `server`，通过包名划分逻辑分层。依赖方向（包间）：

```
server → application → domain → common
infrastructure → domain
```

- **common**（`com.agentcache.common`）：通用异常、响应封装、工具类。不依赖任何业务包。
- **domain**（`com.agentcache.domain`）：实体、枚举、Repository 接口、Port 接口。承载领域模型。
- **application**（`com.agentcache.application`）：Service 层、用例编排、应用 DTO。依赖 domain 与 common。
- **infrastructure**（`com.agentcache.infrastructure`）：存储适配器（Local/COS）、配置加密等 Port 实现，仅实现 domain 中定义的 Port。
- **server**（`com.agentcache.server`）：启动入口、全局配置、Controller、Web DTO、全局异常处理、Spring Security 配置。

> 说明：Repository 接口位于 domain 包并直接继承 `JpaRepository`（采用 Spring Data JPA 的接口即实现方式），因此无需独立的 JPA 实现包。domain 以贫血模型 + Spring Data JPA 为准，不刻意保持框架无关。

**边界约束**：

- Service 只注入 Repository 和 Port，不直接调用 Infrastructure 实现
- 跨聚合操作通过领域事件解耦
- 禁止在一个 Service 中直接操作另一个聚合的 Repository
- 对外 API 不得暴露内部领域类型

---

## 编码规范

### Java 通用

- 使用 Java 17，Spring Boot 3.3
- **禁止使用 Java record**，所有数据载体使用 class
- 使用 Lombok 简化样板代码：`@Data`、`@Value`、`@Getter`、`@Setter`、`@Slf4j`、`@Builder` 等
- JPA 实体使用 `@Getter` + `@Setter` + `@NoArgsConstructor` + `@AllArgsConstructor`，避免 `@Data`
- 类必须有类级 Javadoc，说明职责
- 非显而易见的方法添加简短注释
- 禁止使用 `System.out.println` 或 `e.printStackTrace()`

### 命名规则

- 类名 PascalCase，方法/变量 lowerCamelCase，常量 UPPER_SNAKE_CASE
- 数据库列名、JSON 字段、JPA 实体字段全部使用 camelCase
- REST URL 使用 kebab-case，如 `/api/file-records/{id}`
- 字段名/方法名不必重复类名：
  - 推荐：`file.originalName`、`fileService.upload()`
  - 避免：`file.fileOriginalName`、`fileService.uploadFile()`
- Service 注入主聚合 Repository 时，字段命名为 `repository`
- 布尔字段不带 `is/has` 前缀，留给 getter

### 分层与职责

```
Controller → Service → Repository
                ↕
           Port → Infrastructure
```

- Controller 只做参数校验和响应转换，不写业务逻辑
- Service 是业务逻辑的唯一归属
- Repository 只做数据访问
- Port 定义在 domain 包，Infrastructure 实现解耦

### 异常处理

- Service 层只抛领域异常：`ResourceNotFoundException`、`ValidationException`、`DuplicateException`、`InvalidStateException` 等
- 禁止在 Service 层抛 `ResponseStatusException`
- `GlobalExceptionHandler` 统一将领域异常转换为 HTTP 响应

### 事务与事件

- 跨聚合操作通过领域事件解耦
- 长耗时操作（如大文件上传后的异步处理）在事务外执行
- `@TransactionalEventListener` 方法不得同时标注 `@Transactional`

---

## API 规范

- RESTful 风格，统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 分页请求使用 `page` + `size`，响应包含 `total` + `content`
- 错误响应由 `GlobalExceptionHandler` 统一生成
- 公开端点使用 `/public/**` 前缀，允许匿名访问
- 受保护端点使用 `/api/**` 前缀，需认证

---

## 前端规范

- 一个 `.vue` 文件一个组件，文件名 PascalCase，如 `FileCard.vue`
- Store 按业务域拆分，放在 `src/stores/`
- API 调用统一封装在 `src/services/`，使用 `httpClient.ts` 作为 Axios 实例
- 所有 API 响应和组件 Props 必须有 TypeScript 类型定义
- 使用 Scoped CSS，禁止随意使用 `any` 和内联样式

---

## 数据库规范

- MySQL 8.4，schema 仅通过 Flyway 管理
- `ddl-auto: validate`，禁止自动建表
- Flyway 迁移脚本列名使用 camelCase
- 枚举在数据库中存储为 VARCHAR，由 JPA `@Enumerated(EnumType.STRING)` 映射

---

## 存储规范

- 存储后端通过 `FileStoragePort` 抽象
- 逻辑路径格式：`{spaceId}/{fileId}` 或 `{spaceId}/{fileId}/{version}`
- 本地存储必须校验最终路径落在 `basePath` 下
- COS 等云存储使用预签名 URL 或代理下载
- 敏感配置（COS SecretId/SecretKey）加密存储于数据库

---

## 安全规范

- 密码使用 BCrypt 加密
- JWT Secret 和配置加密密钥通过环境变量注入
- API Key 数据库只存 hash，明文仅创建时返回一次
- 文件名必须消毒，禁止路径遍历
- 日志中禁止打印 API Key、密码、SecretKey 等敏感信息
- PUBLIC 文件通过 `/public/**` 开放，PRIVATE 文件必须鉴权

---

## 测试规范

- 后端：JUnit 5 + Mockito，测试类名以 `Tests` 结尾
- 前端：Vitest + Vue Test Utils，测试文件以 `.test.ts` 结尾
- 核心业务逻辑必须有单元测试
- 新增功能需配套测试，修改已有代码前确保相关测试通过

---

## Git 提交规范

- 使用 Conventional Commits：`type(scope): description`
- 常用 type：`feat`、`fix`、`refactor`、`test`、`docs`、`chore`
- 一个提交只做一件事，保持原子性
- 提交前确保代码可编译、测试通过

---

## 参考文档

- 设计文档和实施计划见 `docs/` 目录
