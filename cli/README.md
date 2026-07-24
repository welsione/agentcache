# AgentCache CLI

Java 命令行工具，用于与 AgentCache 后端交互。基于 Picocli + Apache HttpClient 5 + Jackson。

## 构建

```bash
mvn clean package -DskipTests
```

构建产物：`cli/target/agentcache.jar`。

## 运行

```bash
java -jar cli/target/agentcache.jar <command> [options]
```

查看所有命令：

```bash
java -jar cli/target/agentcache.jar --help
```

## 配置文件

位置：`~/.agentcache/config.yaml`

格式：

```yaml
server:
  url: http://localhost:8080
auth:
  apiKey: ak-xxxxxxxx
  defaultSpace: 1
```

> API Key 由 Web 端创建；CLI 仅消费，不创建。

## 典型工作流

```bash
# 1. 配置凭证（API Key 从 Web 端获取）
java -jar cli/target/agentcache.jar login ak-xxx --server http://localhost:8080 --space 1

# 2. 查看空间列表
java -jar cli/target/agentcache.jar spaces list

# 3. 列出文件
java -jar cli/target/agentcache.jar files list

# 4. 上传
java -jar cli/target/agentcache.jar files upload ./report.pdf

# 5. 生成分享链接
java -jar cli/target/agentcache.jar files share 42
```

## 全局选项

- `--json`：所有输出以 JSON 形式打印到 stdout（错误走 stderr）。

## 退出码

- `0`：成功
- `1`：业务错误（HTTP 错误、CLI 调用异常）
- `2`：参数 / 配置错误（未登录、未指定空间、URL 非法等）

## 子命令

| 命令 | 说明 |
| --- | --- |
| `login <apiKey>` | 写入本地配置 |
| `logout` | 删除本地配置 |
| `config show` | 显示当前配置（API Key 脱敏） |
| `config set-server <url>` | 修改 server.url |
| `spaces list` | 列出当前 API Key 可见的空间 |
| `files list [--query Q] [--space ID] [PAGE]` | 分页列出文件 |
| `files upload <localPath> [--space ID]` | 上传文件 |
| `files download <fileId> --output DIR [--space ID]` | 下载文件 |
| `files info <fileId> [--space ID]` | 查看文件元数据 |
| `files link <fileId> [--space ID]` | 输出访问 URL |
| `files share <fileId> [--space ID]` | 设为 PUBLIC 并打印链接 |
| `files private <fileId> [--space ID]` | 设为 PRIVATE |
| `files delete <fileId> [--space ID]` | 删除文件 |