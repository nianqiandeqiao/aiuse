# 免费云平台部署指南

目标：把系统部署到公网，得到一个 `https://xxx.onrender.com` 这样的网址，别人直接打开就能用。

> 为什么要用云平台而不是之前的临时隧道：**云平台自带 https**，浏览器才允许调用麦克风，语音功能在线上反而比局域网更完整；而且七乘二十四小时在线，不依赖你的电脑开着。

## 一、先准备好两样东西

### 1. 一个 MySQL 数据库（免费）

系统用的是 MySQL，推荐 **TiDB Cloud Serverless**（免费的 MySQL 兼容数据库，5GB 存储）：

1. 打开 <https://tidbcloud.com> → 用邮箱或 GitHub 注册 → 创建一个 **Serverless** 集群（选免费套餐）；
2. 集群建好后点 **Connect**，会给你：主机名、端口（4000）、用户名、密码；
3. 再点 **SQL Editor**，执行一句建库：
   ```sql
   CREATE DATABASE med_reminder CHARACTER SET utf8mb4;
   ```
4. 记下这四个值，待会要填到环境变量里。

> 备选：`db4free.net`（邮箱注册即用，200MB，但稳定性一般，适合临时演示）。不要用 Render 自带的 Postgres —— 我们的建表语句是为 MySQL 写的，换成 Postgres 要改表结构。

### 2. 一个能访问的代码仓库

Render 部署时要从 Git 仓库拉代码。你的代码在 Gitee：

1. 打开 <https://gitee.com/nianqingdeqiaojiangjun/aiuse> → 「管理」→ 把仓库**设为开源/公开**（演示期可以，答辩结束再改回私有）；
2. 复制仓库的 HTTPS 地址备用。

> 不想公开仓库的话，另一条路是把镜像推到 Docker Hub（需要 Docker Hub 账号），再在 Render 里选 "Deploy an existing image"。其实公开一个演示仓库没风险，仓库里**没有任何密钥**。

## 二、部署到 Render（免费，推荐）

1. 打开 <https://render.com> → 用 GitHub 或邮箱注册（免费）；
2. 右上角 **New +** → **Web Service**；
3. 选 **Public Git repository**，粘贴你的 Gitee 仓库地址 → Continue；
4. 关键设置：
   - **Runtime**：`Docker`（仓库里有 Dockerfile，会自动识别）
   - **Instance Type**：`Free`
   - **Health Check Path**：`/api/health`
5. 在 **Environment Variables** 里加下面这些（对照填你自己的值）：

| 变量名 | 填什么 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `cloud` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<TiDB主机>:4000/med_reminder?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true&sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | TiDB 给你的用户名 |
| `SPRING_DATASOURCE_PASSWORD` | TiDB 给你的密码 |
| `BAIDU_OCR_API_KEY` | 你的百度 API Key |
| `BAIDU_OCR_SECRET_KEY` | 你的百度 Secret Key |
| `LLM_ENABLED` | `false`（有千帆 Key 再改 `true` 并加 `LLM_API_KEY`） |
| `JAVA_TOOL_OPTIONS` | `-Xmx320m`（免费实例只有 512MB 内存，必须限一下） |

6. 点 **Create Web Service** → 等 5~10 分钟构建（第一次要装依赖、编译前端和后端）；
7. 构建完成后，页面顶部会给出网址，形如 `https://med-reminder-xxxx.onrender.com`。

打开这个网址 → 注册账号 → 就能用了。数据库表会在首次启动时自动创建。

## 三、免费实例的三个注意事项

1. **会休眠**：免费实例 15 分钟没人访问就会睡眠，下次打开要等 30~60 秒才醒（页面白屏一会儿是正常的，多等一会）。演示前**提前 1 分钟打开一次**把它唤醒。
2. **内存小**：512MB，所以必须设 `JAVA_TOOL_OPTIONS=-Xmx320m`，上传图片别超过 4MB。
3. **国内访问偏慢**：Render 服务器在国外，国内打开会有 2~5 秒延迟，图片上传慢一些。要求更高就用下面的国内方案。

## 四、想要国内速度（备选，不免费）

腾讯云/阿里云的**学生机**（约 10 元/月）或轻量应用服务器（约 24 元/月），国内访问快且稳定：

1. 买一台 2核2G 的轻量服务器，系统选 Ubuntu 22.04；
2. 装 Docker：`curl -fsSL https://get.docker.com | sh`；
3. 把仓库拉下来：`git clone <仓库地址> && cd aiuse`；
4. 一条命令起全站（自带 MySQL）：
   ```bash
   docker compose up -d --build
   ```
5. 浏览器打开 `http://服务器公网IP:8081/` 即可。

> 想绑自己的域名，国内服务器需要 ICP 备案（约 1~2 周）；只用 IP 访问不用备案。

## 五、本地也能用同一套方式验证

装了 Docker 的电脑上，在项目根目录执行：

```bash
docker compose up -d --build
```

就会同时起 MySQL 和系统，浏览器打开 <http://localhost:8081/>。这套配置和云平台上的完全一致，本地验证通过再上云就不会出意外。

## 六、常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 构建失败，卡在 `npm ci` | 检查 `frontend/package-lock.json` 是否提交到仓库（本项目已提交） |
| 启动报数据库连接失败 | 检查 `SPRING_DATASOURCE_URL` 的库名是否存在（先用 SQL Editor 建库）、是否 `useSSL=true` |
| 启动被杀（Out of memory） | 确认设了 `JAVA_TOOL_OPTIONS=-Xmx320m` |
| 打开很慢/白屏 | 免费实例休眠唤醒中，等 30~60 秒 |
| 语音提问不可用 | 云端是 https，麦克风没问题；但仍需在百度控制台开通「短语音识别」并填好两个百度密钥 |

