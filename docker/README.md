# AgentCache Docker Compose 部署

## 前置条件

- Docker
- Docker Compose

## 启动

进入 `docker/` 目录，复制环境变量示例并替换其中的密码和密钥：

```bash
cd docker
cp .env.example .env
# 编辑 .env，替换所有 change-me 值
docker-compose up -d --build
```

Web 管理面板默认监听宿主机的 **80** 端口。MySQL 使用容器内的 **3306** 端口，不暴露到宿主机。

默认管理员账号：

- 用户名：`admin`
- 密码：`admin@123`

首次登录后请立即修改默认密码。

## 数据持久化

数据保存在 Docker named volumes 中：

- MySQL：`agentcache_mysql-data`
- 文件存储：`agentcache_file-data`

实际 volume 名可能因 Compose 项目名或 `-p` 参数而改变，可用 `docker volume ls` 查看。

## 常用命令

升级镜像并重新构建、启动服务：

```bash
docker-compose pull
docker-compose up -d --build
```

查看 API 日志：

```bash
docker-compose logs -f api
```

停止服务并保留数据：

```bash
docker-compose down
```

停止服务并清除所有数据：

```bash
docker-compose down -v
```

> `docker-compose down -v` 会永久删除 MySQL 和文件存储数据，请谨慎使用。
