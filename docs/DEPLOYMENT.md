# 云服务器部署说明

## 1. 推荐环境

- Ubuntu 22.04 LTS 或 24.04 LTS
- 2 核 CPU
- 2 GB 内存起步，推荐 4 GB
- 40 GB 以上磁盘
- Docker Engine
- Docker Compose Plugin
- Nginx
- 一个解析到服务器的域名

部署结构：

```text
Internet
  ↓ 80/443
Nginx
  ↓ 127.0.0.1:8080
Spring Boot Docker 容器
  ↓
PostgreSQL Docker 容器
```

## 2. 创建生产环境文件

```bash
cd /opt/review-desk
cp .env.production.example .env.production
chmod 600 .env.production
```

至少修改：

- `POSTGRES_PASSWORD`
- `DATABASE_PASSWORD`
- `DEMO_TEACHER_PASSWORD`
- `DEMO_STUDENT_PASSWORD`

`POSTGRES_PASSWORD` 与 `DATABASE_PASSWORD` 必须一致。

`.env.production` 不得提交到 Git。

## 3. 启动应用和数据库

```bash
docker compose --env-file .env.production up -d --build
```

检查状态：

```bash
docker compose ps
docker compose logs -f app
```

本地健康检查：

```bash
curl http://127.0.0.1:8080/login
```

## 4. 安装和配置 Nginx

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
```

复制配置：

```bash
sudo cp deploy/nginx-review-desk.conf /etc/nginx/sites-available/review-desk
sudo ln -s /etc/nginx/sites-available/review-desk /etc/nginx/sites-enabled/review-desk
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

把配置中的：

```text
review.example.com
```

替换为实际域名。

## 5. 配置 HTTPS

```bash
sudo certbot --nginx -d review.example.com
```

证书续期检查：

```bash
sudo certbot renew --dry-run
```

## 6. 防火墙

只开放：

- `22/tcp`：SSH，建议限制为管理 IP
- `80/tcp`：HTTP 和证书验证
- `443/tcp`：HTTPS

不要对公网开放 PostgreSQL `5432` 或应用内部端口 `8080`。

## 7. 更新版本

```bash
git pull
docker compose --env-file .env.production up -d --build
```

## 8. 数据备份

备份 PostgreSQL：

```bash
docker compose exec -T db pg_dump \
  -U review_desk review_desk \
  > review-desk-$(date +%F).sql
```

恢复前先停止应用，并确认备份文件有效。

Docker 数据保存在命名卷 `review-desk-db` 中，不要执行：

```bash
docker compose down -v
```

该命令会删除数据库卷。

## 9. 日志和排查

```bash
docker compose logs --tail=200 app
docker compose logs --tail=200 db
sudo nginx -t
sudo journalctl -u nginx --no-pager -n 100
```

## 10. 首次启动后的检查

1. 打开域名登录页。
2. 分别登录教师和学生账号。
3. 创建课堂、发布作业、学生提交。
4. 教师填写反馈并点击“发布反馈”。
5. 学生重新登录，确认能查看提交全文和最终反馈。
6. 重启容器后确认数据仍然存在。

