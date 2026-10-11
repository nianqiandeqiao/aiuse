# 用 Render 免费部署成固定网址（手把手版）

> 目标：得到一个形如 `https://med-reminder-xxxx.onrender.com` 的**固定网址**，
> 24 小时在线、自带 https、**不依赖你自己电脑开机**，麦克风语音也能用。
>
> 这条路完全免费，代价是免费实例 15 分钟没人访问会休眠，下次打开要等 30~60 秒。

---

## 零、先看清楚：三个角色分别是谁

| 角色 | 干什么 | 花钱吗 | 谁来做 |
| --- | --- | --- | --- |
| **GitHub** | 存代码，Render 从它拉代码 | 免费 | ✅ 已经替你建好了 |
| **TiDB Cloud** | 提供 MySQL 数据库（存老人/用药/打卡数据） | 免费 5GB | 👉 你做（第一步） |
| **Render** | 跑我们的程序，给出公网网址 | 免费 | 👉 你做（第二步） |

代码仓库地址（已建好，可直接打开确认）：
<https://github.com/nianqiandeqiao/aiuse>

> 为什么不用 Gitee？Render 只认 GitHub / GitLab / Bitbucket 三家，粘 Gitee 地址会报
> `Invalid repository URL`。所以我把 Gitee 的代码镜像了一份到你的 GitHub 账号下，
> 仓库里**没有任何密钥**（数据库密码、百度密钥都走环境变量），公开也不会有风险。

⚠️ **顺序不能反**：先拿到数据库，再去 Render 建服务。否则 Render 第一次启动会因为连不上数据库而失败。

---

## 第一步：拿一个免费 MySQL（约 8 分钟）

1. 打开 <https://tidbcloud.com>，点右上角 **Start for Free**；
2. 选 **Sign in with GitHub**（你已经登录过 GitHub，这一步点一下就好，不用再记新密码）；
3. 进去后点 **Create Cluster**（创建集群），按下面填：

   | 项目 | 选什么 |
   | --- | --- |
   | Cluster Name | `med-reminder` |
   | Cloud Provider / Region | **AWS / Singapore**（新加坡，离国内最近，最快） |
   | Plan | **Starter**（这就是免费的那档，别选 Essential / Dedicated，那两个要钱） |

4. 点 **Create**，等 1~3 分钟，状态变成 **Available / Active** 就算好了；
5. 点 **Connect** 按钮 → 选 **MySQL Client**（或 General）→ 页面上会给出四个值，
   **请复制到记事本存好**：

   | 名字 | 长这样 |
   | --- | --- |
   | Host | `gateway01.ap-southeast-1.prod.aws.tidbcloud.com` |
   | Port | `4000` |
   | User | `3xxxxxxxxxxxxxx.root` |
   | Password | 一串随机字符 |

   > 🔴 **密码只显示一次**，没记下来就得重置，先存记事本。

6. 点左侧 **SQL Editor**，把下面这句粘进去执行（建数据库）：

   ```sql
   CREATE DATABASE med_reminder CHARACTER SET utf8mb4;
   ```

   执行成功（不报红）就行。**表不用你建**，程序第一次启动会自己建表并写入 11 条药物冲突规则。

---

## 第二步：在 Render 上部署（约 10 分钟，其中构建 8~15 分钟）

1. 打开 <https://render.com>，点 **Get Started** → 用 **GitHub** 登录（第一次会让你授权，点同意即可）；
2. 登录后点右上角 **New +** → 选 **Blueprint**；
3. 第一次会跳转到 GitHub 授权页面，选 **Only select repositories** → 只勾 **aiuse** → 点 **Install**；
4. 回到 Render，列表里会出现 `nianqiandeqiao/aiuse`，点它右边的 **Connect / Apply**；
5. Render 会自动读取仓库里的 `render.yaml`，然后弹出两个要你填的输入框（其他都已自动配好）：

   | 变量名 | 填什么 |
   | --- | --- |
   | `SPRING_DATASOURCE_URL` | `jdbc:mysql://你的Host:4000/med_reminder?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true&sslMode=REQUIRED` |
   | `SPRING_DATASOURCE_USERNAME` | TiDB 给你的 User（形如 `3abcxxxx.root`） |
   | `SPRING_DATASOURCE_PASSWORD` | TiDB 给你的 Password |
   | `BAIDU_OCR_API_KEY` | 你自己的百度 API Key，见下面的说明 |
   | `BAIDU_OCR_SECRET_KEY` | 你自己的百度 Secret Key，见下面的说明 |

   > 把 `你的Host` 换成第一步记下的真实 Host，**前后不要留空格**。
   >
   > 参数说明（照抄就行，别自己删）：
   > - `sslMode=REQUIRED`：TiDB Cloud 强制加密连接，缺了会连不上；
   > - `connectionTimeZone` + `forceConnectionTimeZoneToSession`：TiDB 服务器时区是 UTC，
   >   不加这两项的话，入库时间会比北京时间**晚 8 小时**；
   > - `characterEncoding=utf8`：保证中文不乱码。
   >
   > 🔐 **百度密钥从哪来**：打开本机文件 `backend/src/main/resources/application-local.yml`，
   > 里面 `api-key` 和 `secret-key` 两行就是要填的值（这个文件被 `.gitignore` 忽略，
   > 只存在于你自己电脑上，**不要**把它复制进仓库、不要把密钥贴到聊天群或文档里）。
   > 填到 Render 的环境变量里即可，环境变量不会出现在公开的代码仓库中。

6. 点 **Apply**（或 **Create Resources**），开始构建。
   - 第一次要装前端依赖 + 编译后端，大约 **8~15 分钟**，页面停在 `Build in progress` 是正常的；
   - 构建成功后会显示 **Live**，页面上方就是你的网址。

如果不想用 Blueprint，也可以走手动路线：**New +** → **Web Service** → **Public Git repository**
→ 粘 `https://github.com/nianqiandeqiao/aiuse` → Runtime 选 `Docker`、Instance Type 选 `Free`、
Health Check Path 填 `/api/health` → 在 Environment 里把上面那几个变量一条条加进去 → Create。

---

## 第三步：打开网址，注册账号

1. 打开 Render 给你的网址（形如 `https://med-reminder-xxxx.onrender.com`）；
2. **第一次打开可能要等 30~60 秒**（免费实例在唤醒），白屏别慌，等它；
3. 点「注册」，自己设一个用户名密码 → 登录；
4. 登录后是一套**干净的空系统**，你自己加老人、加药就行。

> 有一点要提前知道：**每个人只能看到自己账号下的数据**。所以你把网址发给同学，
> 他们看到的是他们自己的空系统；演示给老师看，要用**你自己注册的那个账号**登录，
> 才能看到你录入的演示数据（张三 + 阿司匹林/华法林 红色冲突）。

---

## 免费版的三个注意点（答辩前必看）

1. **会休眠**：15 分钟没人访问就睡，下次打开等 30~60 秒。**答辩前提前 1 分钟打开一次**把它唤醒，
   或者用 <https://uptimerobot.com> 这类免费监控每 5 分钟访问一次 `/api/health` 让它不睡。
2. **内存只有 512MB**：所以 `render.yaml` 里已经写了 `JAVA_TOOL_OPTIONS` 限制 JVM 内存，
   **不要删掉那一行**；上传药盒图片别超过 4MB。
3. **国内访问偏慢**：Render 服务器在美国/新加坡，国内打开有 2~5 秒延迟，正常现象。
   想更快更稳，就买一台国内云服务器，见 [deploy-server.md](deploy-server.md)。

---

## 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| Render 粘 Gitee 地址报 `Invalid repository URL` | Render 不认 Gitee，用本指南的 GitHub 地址 |
| 部署后打开报 502 / Application failed to start | 十有八九是数据库变量填错。去 Render 左侧 **Logs** 看最后几行，出现 `Communications link failure` 就是 Host/端口/密码不对 |
| 日志里 `Unknown database 'med_reminder'` | 第一步第 6 步的建库 SQL 没执行成功，回 TiDB SQL Editor 重跑 |
| 日志里 `Access denied` | 用户名/密码复制错了，或密码没重置过；回 TiDB 重置密码后更新 Render 变量 |
| 构建失败，卡在 `npm ci` | 多试一次（网络抖动）。仓库里 `package-lock.json` 是提交过的，不用管 |
| 打开很慢 / 白屏 | 免费实例休眠唤醒中，等 30~60 秒 |
| 改了代码怎么上线 | `git push` 到 GitHub 的 `master` 分支，Render 会自动重新构建部署 |

---

## 以后改代码怎么同步到线上

本机改完代码后，双击项目根目录的 **`推送到GitHub.bat`**（或执行下面两行），
Render 检测到新提交会自动重新构建：

```powershell
git add -A
git commit -m "你的修改说明"
git push github master
```

> 注意：`origin` 是 Gitee（团队协作看代码用），`github` 是 Render 部署用，两个都要推的话分别 push 一次。
