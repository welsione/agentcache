# AgentCache

AgentCache 是一个面向小团队的文件中转与交接服务。管理员可以通过 Web 面板管理文件、空间和 API Key；Agent 可以通过本地 CLI 上传、下载和分享文件。文件支持私有（PRIVATE）和公开（PUBLIC）两种可见性，并可在本地磁盘与腾讯云 COS 两种存储后端之间切换。

---

## 功能特性

- **Web 管理面板**：空间管理、文件上传/下载/删除、API Key 管理、系统配置。
- **Agent CLI**：基于 API Key 登录，支持 spaces、files 等命令，方便脚本和 Agent 集成。
- **文件上传与下载**：支持 multipart 上传、直链下载、公开/私有访问控制。
- **空间隔离**：每个空间独立管理成员和文件，支持 MANAGER / MEMBER / READER 三种空间角色。
- **存储后端**：本地磁盘或腾讯云 COS，可在管理面板内切换。
- **权限控制**：JWT 用户认证 + API Key 认证，PUBLIC 文件可无鉴权访问。

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
├── backend/              # Spring Boot 后端（单模块，按包分层）
│   └── server/           # 启动 + Web/应用/领域/基础设施各层
├── frontend/             # Vue 3 前端
├── cli/                  # Java CLI
├── docker/               # Docker Compose 与镜像文件
├── docs/                 # 设计文档
└── README.md             # 本文件
```

---

## 快速开始（Docker Compose）

### 前置条件

- Docker
- Docker Compose

### 启动

```bash
cd docker
cp .env.example .env
# 编辑 .env，将所有 change-me 替换为强随机值
# JWT_SECRET 和 AGENTCACHE_CONFIG_KEY 建议 >= 32 字节
docker-compose -f docker/docker-compose.yml up -d --build
```

Web 管理面板默认监听宿主机的 **80** 端口，访问：

```
http://localhost
```

默认管理员账号：

- 用户名：`admin`
- 密码：`admin@123`

首次登录后请立即修改默认密码。

### 常用命令

```bash
# 查看 API 日志
docker-compose -f docker/docker-compose.yml logs -f api

# 停止并保留数据
docker-compose -f docker/docker-compose.yml down

# 停止并清除数据（谨慎使用）
docker-compose -f docker/docker-compose.yml down -v
```

---

## 开发模式

### 后端

需要本地 MySQL 8.4，创建数据库 `agentcache` 并配置环境变量：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=agentcache
export MYSQL_PASSWORD=your-password
export JWT_SECRET=your-32-byte-secret
export STORAGE_TYPE=local
export STORAGE_LOCAL_BASE_PATH=/tmp/agentcache

cd backend
mvn spring-boot:run -pl server -am
```

后端默认运行在 `http://localhost:8080`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`。

### CLI

```bash
cd cli
mvn package -DskipTests
java -jar target/agentcache.jar login ak-xxx
java -jar target/agentcache.jar spaces
java -jar target/agentcache.jar files --help
```

---

## API 摘要

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 端点 | 说明 | 认证 |
|------|------|------|
| `POST /api/auth/login` | 用户登录，返回 JWT | 公开 |
| `GET /api/spaces` | 列出当前用户/API Key 可见的空间 | JWT / API Key |
| `POST /api/spaces` | 创建空间 | JWT |
| `GET /api/spaces/{id}/files` | 文件列表 | JWT / API Key |
| `POST /api/spaces/{id}/files` | 上传文件 | JWT / API Key |
| `GET /api/files/{id}/content` | 下载私有文件 | JWT / API Key |
| `PUT /api/files/{id}/visibility` | 切换可见性 | JWT / API Key |
| `DELETE /api/files/{id}` | 删除文件 | JWT / API Key |
| `POST /api/spaces/{id}/api-keys` | 创建 API Key | JWT |
| `GET /public/files/{id}/content` | 公开下载 | 公开 |

完整 API 与权限设计见项目 `docs/` 目录下的设计文档。

---

## 配置

系统配置（存储方式、COS 凭据、文件限制等）在登录 Web 管理面板后，通过左侧菜单的 **系统配置** 进行管理。

- **存储类型**：`local` 或 `cos`
- **本地存储路径**：宿主机文件系统路径
- **COS 配置**：SecretId、SecretKey、Bucket、Region 等（SecretKey 加密存储）

---

## 安全注意事项

- **首次部署后立即修改默认管理员密码**。
- 使用 HTTPS 对外提供服务，避免 JWT 和 API Key 在传输中被窃听。
- 修改 `docker/.env` 中的默认 `JWT_SECRET` 和 `AGENTCACHE_CONFIG_KEY`，长度建议 >= 32 字节。
- API Key 仅在创建时显示一次，请妥善保存；泄露后应立即在管理面板吊销。
- 定期备份 MySQL 数据库和文件存储卷。

---

## License

MIT
