# AI 用药提醒与用药安全助手

国庆 6 天冲刺项目（10 月 2 日 — 10 月 7 日），目标：做出一版能现场演示的雏形。

## 队友上手（刚克隆下来？先看这里）

代码里**不含任何密钥**（数据库密码、百度密钥都在被忽略的本地配置文件里），所以拉下来后需要自己补一份配置才能跑。两种方式，挑一种：

### 方式一：装了 Docker（最省事，推荐）

在项目根目录执行一条命令，会自动装好数据库、构建前后端并启动：

```bash
docker compose up -d --build
```

等 5~10 分钟构建完成，浏览器打开 <http://localhost:8081/> 即可。
数据库密码、百度密钥想改的话，在根目录建一个 `.env` 文件（参照 `deploy/install-server.sh` 里生成的那几个变量）。

### 方式二：不用 Docker，手动跑

**前置环境**：JDK 17、Maven 3.9+、MySQL 8/9、Node.js 20+（只跑后端托管模式的话可以不用 Node）。

```bash
# 1) 建库（MySQL 里执行一次）
#    CREATE DATABASE med_reminder CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

# 2) 补本地配置：复制模板并填上自己的数据库密码
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
#    编辑 application-local.yml：datasource.password 填你的 MySQL 密码
#    （百度密钥没有也能跑，只是拍照识药不可用）

# 3) 启动后端（表结构会在启动时自动创建）
cd backend && mvn spring-boot:run

# 4) 另开一个终端，构建前端并交给后端托管
cd frontend && npm install && npm run build
#    然后打开 http://localhost:8081/
```

Windows 用户也可以直接双击仓库里的 `启动系统.bat`（会自动构建前端并启动后端）。

> 开发前端时用 `npm run dev`（5173 端口，自动代理到后端 8081），改代码即时生效。

### 跑不起来时先看这几条

| 现象 | 原因 |
| --- | --- |
| 启动报数据库连接失败 | `application-local.yml` 没建，或密码填错，或库 `med_reminder` 不存在 |
| 提示端口 8081 被占用 | 已经有一个实例在跑，先关掉旧窗口 |
| 页面能打开但接口全报 401 | 正常现象：系统是账号制，先注册一个账号再操作 |
| 拍照识药提示未配置密钥 | 没填百度 OCR 的 API Key / Secret Key |
| 语音提问不可用 | 需要 https 或 localhost 打开，且百度控制台要开通「短语音识别」 |

## 项目亮点

1. **拍照识药**：拍药盒照片自动识别药名，并用药品别名词典把"拜阿司匹灵""阿司匹林肠溶片"统一归一到"阿司匹林"
2. **用药安全提醒（核心创新点）**：自动检查老人当前全部用药的相互作用与重复用药，每条冲突都给出严重程度、机制说明、可执行建议和说明书依据
3. **适老化闭环**：拍照识药（不用打字）→ 今日服药清单（按时段排好）→ 一键打卡 → 语音播报
4. **账号制与数据隔离**：每个账号只能查看和修改自己添加的老人、用药计划与打卡记录，别人的数据查不到也改不了
5. **到点语音提醒 + 语音对话**：网页到点自动弹窗并播报"该吃药了"，老人对着麦克风说"吃了/等一会儿"就能应答，类似给老人打了一通电话
6. **问药师（大模型 + 规则双模式）**：可以语音问「这个药能和感冒药一起吃吗」「饭前吃还是饭后吃」；不填大模型 Key 时用药品词典+相互作用规则库回答，有据可查
7. **服药周报/月报**：柱状图 + 大字报数，子女一眼看到「这周漏服 3 次」，医生能看每种药的依从性
8. **药品有效期与剩余量提醒**：临期、过期、快用完、已用完都会提醒（老人爱囤药、忘过期）
9. 分层规范、测试完备：`controller → service → mapper → entity`，32 个 JUnit 测试 + 84 项端到端断言

## 账号与数据隔离（便民设计）

- 打开系统先登录/注册：**用户名 + 密码**。注册只需填用户名和密码（称呼选填），注册成功后自动登录、直接进首页，不用来回跳页面
- 每个账号只管理自己的老人：老人档案列表/详情/修改/删除、用药计划、打卡记录、冲突检查**全部按账号隔离**
- 访问别人的数据统一返回"不存在"（404），而不是"无权限"，避免泄露别人有没有这份档案
- 密码用 PBKDF2 加盐哈希（12 万次迭代）存储，数据库里看不到明文
- 便民细节：记住上次登录的用户名（下次只输密码）、登录状态保持 7 天、会话过期自动跳回登录页并给中文提示、按钮与输入框按适老化放大
- 第一个注册的账号会自动接管开发阶段的历史演示数据（张三等），之后注册的账号都是全新空白账号
- 若不想让陌生人自助注册，把 `application.yml` 里的 `app.auth.allow-register` 改为 `false` 即可

## 无障碍与语音交互（系统性设计，不是简单调大字号）

| 能力 | 实现 |
| --- | --- |
| 超大字号 + 高对比度 + 大按钮 | 右上角一键切换「标准字 / 超大」，字号、行距、按钮高度全部放大；深蓝配明黄高对比配色 |
| 全程语音引导 | 到点自动语音播报服药提醒；问药回答可以点「读给我听」；打卡成功也会语音反馈 |
| 一键式操作 | 提醒弹窗里只有三个大按钮：已经吃了 / 等一会儿 / 我要说话，老人不用打字 |
| 支持方言 | 语音识别走浏览器能力，提问时可选择河南话（内置河南话词表归一，如"喝药→吃药""中不中→行不行"）；四川话、粤语可扩展同一套词表 |
| 子女远程代录 | 一个账号可以建多位老人档案，子女登录后替老人录入用药、设置提醒；老人自己的账号只看到自己的数据 |
| 历史用药留存 | 药品停用或删除后，用药记录仍然保留，随时可以在「用药计划 → 历史用药」回查 |

## 大模型接入说明

系统默认使用**内置规则引擎**（药品词典 + 11 条相互作用规则）回答问药，不填任何 Key 也能用。
要升级成真大模型对话，任选一个 OpenAI 兼容服务，配三个环境变量即可：

```powershell
$env:LLM_ENABLED="true"
$env:LLM_BASE_URL="https://qianfan.baidubce.com/v2"   # 百度千帆；也可换 DeepSeek/通义/Moonshot
$env:LLM_API_KEY="你的大模型APIKey"
$env:LLM_MODEL="ernie-4.0-8k"
```

> 注意：百度千帆需要用**单独在千帆控制台创建的应用 API Key**，OCR 的密钥不能用于大模型（已实测）。语音识别与播报使用浏览器自带能力，不需要密钥。

## 语音功能的使用条件（重要，答辩前必看）

浏览器的安全策略规定：**只有 https 或 localhost 才允许网页调用麦克风**。所以：

| 打开方式 | 语音提问（麦克风） | 到点提醒弹窗 | 语音播报 | 文字提问 |
| --- | --- | --- | --- | --- |
| 电脑上 `http://localhost:8081` | ✅ 可用 | ✅ | ✅ | ✅ |
| 手机/平板用局域网 IP（如 `http://192.168.43.5:8081`） | ❌ 浏览器禁止 | ✅ | ✅ | ✅ |

也就是说：**在电脑上用 localhost 打开，语音提问是能用的**；手机用局域网 IP 打开时，页面会明确提示"当前用 http 打开，浏览器不允许调用麦克风"，此时改用大按钮或打字即可，其余功能（提醒、播报、拍照识药、报告）都正常。

> 如果一定要让手机也能用语音，需要给内网配 https 证书，本期未启用；手机端建议直接用文字提问。

## 团队分工

| 代号 | 姓名 | 角色 |
| --- | --- | --- |
| A | 乔铄淇 | 后端主程（接口 + OCR + 相互作用引擎） |
| B | 赵恒源 | 前端主程（网页界面 + 交互 + 联调） |
| C | 陈润瑾 | 需求调研 + 演示脚本 + 接口测试 |
| D | 张宁 | 竞品分析 + 创新点 + 前端联调 |
| E | 程诺 | 计划书撰写 |
| F | 郑玲钰 | PPT 制作 + 视觉设计 |

## 技术栈

- 后端：Java 17 + Spring Boot 4.0.0 + MyBatis 3.5.19 + Lombok + MySQL 9.6
- 前端：Vue 3.5 + Vite 7 + Element Plus + vue-router + axios（六个页面已完成）
- 外部能力：百度智能云「通用文字识别」（OCR 识药）
- 访问地址：演示（构建版）统一走 `http://localhost:8081/`；开发态前端 `http://localhost:5173/`
- 后端端口：`8081`，接口前缀 `/api`

## 目录结构

```
aiuse/
├── backend/                 # 后端 Maven 工程
│   ├── src/main/
│   │   ├── java/com/aiuse/medreminder/
│   │   │   ├── common/      # 统一响应体、全局异常
│   │   │   ├── config/      # CORS、建表初始化
│   │   │   ├── controller/  # REST 接口
│   │   │   ├── service/     # 业务逻辑
│   │   │   ├── mapper/      # MyBatis Mapper 接口（持久层）
│   │   │   ├── entity/      # 数据库实体（POJO）
│   │   │   ├── dto/         # 请求参数对象
│   │   │   └── vo/          # 返回给前端的结果对象（今日清单、统计）
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml.example
│   │       ├── schema.sql   # 三张核心表
│   │       └── mapper/      # MyBatis SQL 映射文件（*.xml）
│   └── src/test/java/com/aiuse/medreminder/   # JUnit 单元/接口测试
├── frontend/                # 前端 Vue 3 + Vite 工程
│   ├── src/
│   │   ├── api/             # axios 封装（统一解包 + 错误提示）
│   │   ├── router/          # hash 路由（构建版刷新不 404）
│   │   ├── stores/          # 当前选中老人（localStorage 记忆）
│   │   ├── styles/          # 适老化主题（颜色与字号集中在变量里）
│   │   ├── utils/speech.js  # 浏览器语音播报
│   │   └── views/           # 首页/档案/用药/打卡/识药/安全 六个页面
│   ├── vite.config.js       # 开发代理 + 构建输出到后端 static
│   └── package.json
├── docs/
│   ├── api.md               # 接口清单（给赵、陈）
│   ├── design.md            # 系统设计说明书（架构/数据库/流程/创新点）
│   └── test-report.md       # 测试报告（用例、结果、缺陷记录）
└── scripts/                 # 环境、建库、启动、测试、冒烟脚本
```

## 快速开始

> 小白版（只开一个服务，答辩推荐）：
> 1. 双击 `启动系统.bat` —— 自动构建前端、启动后端，18 秒后自动打开浏览器；
> 2. 黑窗口别关，关掉就等于停服务；
> 3. 想验证功能全绿，再双击 `冒烟测试.bat`。
>
> 开发前端时用 `启动前端.bat`（改代码自动刷新）；只想调接口时用 `启动后端.bat`。
> 下面是用命令行的完整说明。

### 1. 准备数据库

确保本机 MySQL 已启动（默认 `127.0.0.1:3306`），然后创建数据库：

```powershell
$env:MED_DB_PASSWORD = "你的MySQL密码"
powershell -ExecutionPolicy Bypass -File scripts/init-db.ps1
```

### 2. 配置本地数据库密码

```powershell
Copy-Item backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
# 然后编辑 application-local.yml，填入真实密码
```

`application-local.yml` 已被 `.gitignore` 忽略，**不会提交到 Git**，密码不要发群里。

### 3. 启动后端

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-backend.ps1
```

启动成功后访问健康检查：<http://localhost:8081/api/health>

> 注意：本机 PowerShell 处于 Restricted 执行策略，脚本必须用
> `powershell -ExecutionPolicy Bypass -File <脚本>` 运行；直接双击或 `.\scripts\xxx.ps1` 会被拦截。
> 脚本文件刻意保持纯 ASCII，避免 Windows PowerShell 5.1 按 GBK 解析 UTF-8 导致中文乱码/语法错误。

### 4. 冒烟测试（另开一个终端）

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

### 5. 启动前端（开发态，可选）

双击 `启动前端.bat`，或命令行执行：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

浏览器打开 <http://localhost:5173/>。开发态由 Vite 把 `/api` 代理到后端 8081，前端不用处理跨域。

> 注意：本机 PowerShell 的执行策略禁止运行 `npm.ps1`，所以命令要用 **`npm.cmd`** 而不是 `npm`；`启动前端.bat` 里已经处理好了。

### 6. 构建前端（让后端 8081 直接托管）

双击 `构建前端.bat`，或命令行执行：

```powershell
cd frontend
npm.cmd run build
```

构建产物会输出到 `backend/src/main/resources/static/`（已被 .gitignore 忽略，不提交）。之后只要启动后端，打开 <http://localhost:8081/> 就是完整系统，答辩时只需开一个服务。

### 7. 发布到公网给他人访问（可选）

> **推荐：想要固定网址、不依赖自己电脑开机 → [docs/deploy-render.md](docs/deploy-render.md)（手把手版）**
>
> 用 Render（免费）+ TiDB Cloud（免费 MySQL）。代码已镜像到 GitHub 供 Render 拉取：
> <https://github.com/nianqiandeqiao/aiuse>（Render 不认 Gitee，必须走 GitHub）。
> 改完代码双击根目录的 `推送到GitHub.bat` 即可同步上线。
>
> 备选方案（内容更全但步骤略抽象）→ [docs/deploy-cloud.md](docs/deploy-cloud.md)：
> 里面写了用 Render（免费）+ TiDB Cloud（免费 MySQL）的完整步骤，仓库里已经准备好 Dockerfile 和 render.yaml，照着填几个环境变量就能上线。
>
> **已经买了云服务器？** 直接看 → [docs/deploy-server.md](docs/deploy-server.md)：装好系统后两条命令部署完成（含买什么配置、安全组怎么放行、日常运维与备份命令）。

双击 `启动公网访问.bat`：脚本会自动启动后端并建立一个 Cloudflare 免费临时隧道，窗口里会给出形如 `https://xxxx-xxxx.trycloudflare.com` 的公网地址，发给别人就能访问。

安全性说明：系统是**账号制**，每个人用自己的用户名密码注册登录，**只能看到和修改自己添加的老人、用药计划与打卡记录**，别人的数据连查都查不到（未登录访问数据接口一律返回 401，访问别人的数据返回 404）。

限制：免费隧道地址随机且临时，每次重启都会变；你的电脑必须一直开机。更长期的方案（云服务器、HTTPS、开机自启）见 [docs/deploy.md](docs/deploy.md)。

> 不要长期把公网地址挂在外面：真实老人数据属于敏感个人信息，正式使用前还需要加密存储与合规措施。如果不想让陌生人自助注册，可把 `application.yml` 里的 `app.auth.allow-register` 改为 `false`。

## 第 1 天（10 月 2 日）交付与验收

乔（后端）当日任务：搭后端环境、跑通骨架、确认数据库可用。

- [x] Git 仓库初始化，后端工程骨架就绪
- [x] MySQL 数据库 `med_reminder` 创建完成，账号可连通
- [x] 三张核心表：`elder`、`medication_plan`、`medication_log`
- [x] `GET /api/health` 返回 `db=UP` 与 MySQL 版本
- [x] 老人档案（增/查/删）、用药计划（增/查）、服药打卡（增/查）接口可用
- [x] `scripts/smoke-test.ps1` 全链路通过
- [x] `docs/api.md` 接口清单，供赵联调、陈写接口测试

验收命令：

```powershell
# 1) 服务启动后
Invoke-RestMethod http://localhost:8081/api/health
# 2) 全链路冒烟
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

### 第 1 天验收记录（乔，2026-10-02）

- 后端启动成功：Spring Boot 4.0.0 / Spring 7.0.1 / Tomcat 11 / 端口 8080（第 3 天按你的要求改为 8081）
- `GET /api/health` 返回 `status=UP, db=UP, mysqlVersion=9.6.0, schema=med_reminder`
- `scripts/smoke-test.ps1` 全部 10 组用例、19 项断言通过（含中文姓名「张三」写入/回读、重复打卡拦截、缺参 400、未知 id 404、逻辑删除）
- 持久化验证：重启服务后，老人/用药计划/打卡记录仍可查询
- 数据库现有 1 条演示数据（老人「张三」+ 阿司匹林计划 + 今日打卡），方便赵做前端联调

清空演示数据（可选，按外键顺序删除）：

```sql
DELETE FROM medication_log; DELETE FROM medication_plan; DELETE FROM elder;
```

## 第 2 天（10 月 3 日）交付与验收

乔（后端）当日任务：完成「老人档案 + 用药计划 + 打卡」三块后端接口。

- [x] 老人档案：新增 / 列表 / 搜索 / 详情 / 修改 / 删除（删除时级联停用其用药计划）
- [x] 用药计划：新增 / 列表 / 详情 / 修改 / 停用与恢复 / 删除
- [x] 服药打卡：打卡 / 撤销误打卡 / 按日期或区间查询 / 按时率统计
- [x] 今日服药清单 `GET /api/plans/today`：打卡页主接口，直接给出「今天该吃什么、几点吃、吃没吃」
- [x] 参数校验加强：提醒时间格式、年龄范围、计划状态取值、统计区间上限
- [x] `docs/api.md` 更新为第 2 天完成版，赵可直接照着写前端
- [x] `scripts/smoke-test.ps1` 扩到 20 组用例、53 项断言，全部通过（第 3~4 天又扩充为 24 组、68 项）

验收记录（2026-10-03）：`PASS: 53   FAIL: 0`。覆盖中文姓名写入回读、重复打卡拦截（400）、非法提醒时间格式（400）、计划停用后今日清单清空、按时率 0.5 校验、误打卡撤销（删除后再删返回 404）、逻辑删除后查询 404 等。

> 语音播报属于前端实现（浏览器自带 `speechSynthesis` 就能播报，不需要后端接口），由赵负责；若后续决定改用百度语音合成，我再补一个后端代理接口，避免 API Key 暴露在前端。

## 持久层改造：JdbcTemplate → MyBatis（10 月 3 日追加）

按你的要求把持久层换成了课上学的 MyBatis，**接口行为完全不变**（同一套冒烟测试 53 项断言依旧全绿）。改动如下：

- 依赖：`spring-boot-starter-jdbc` → `mybatis-spring-boot-starter` 4.0.0（MyBatis 3.5.19，官方针对 Spring Boot 4.0.0 构建的版本），另加 Lombok
- 实体：`entity` 包三个类由 record 改成 POJO（`@Data`），因为 MyBatis 插入后要回填自增主键 `id`
- 持久层：`repository` 包删除，改为 `mapper` 包下的 `ElderMapper` / `MedicationPlanMapper` / `MedicationLogMapper` 三个接口
- SQL：全部搬到 `src/main/resources/mapper/*.xml`，用 `<resultMap>` 做字段映射，`useGeneratedKeys="true"` 回填主键
- 配置：`application.yml` 增加 `mybatis.mapper-locations` 等配置；启动类加 `@MapperScan("com.aiuse.medreminder.mapper")`
- Service 层只改了取值方式（record 的 `name()` → POJO 的 `getName()`），业务判断逻辑一行未动

分层对照：`controller`（接口）→ `service`（业务）→ `mapper`（SQL）→ `entity`（表结构）。

> 端口说明：`application.yml` 里的 `server.port` 已由 8080 改为 8081（你本地的选择），脚本、文档、健康检查地址已同步更新。

## 第 3 天（10 月 4 日）交付与验收：拍照识药（OCR）

乔（后端）当日任务：接入百度 OCR，写「图片上传 → 识别药名」接口。

- [x] 接入百度智能云「通用文字识别」`general_basic`，access_token 内存缓存（30 天自动续期）
- [x] `POST /api/ocr/drug`：上传药盒照片（multipart，字段名 file），返回原始文字 + 分行 + 命中药品
- [x] `POST /api/ocr/base64`：base64 方式识别，兼容小程序、相机插件拿不到文件流的场景
- [x] 药品别名词典 `drug-dictionary.txt`：35 种常用药、97 个关键词，商品名/剂型自动归一成标准药名，加药只需加一行
- [x] 图片体积校验（>4MB 返回 400）、第三方异常统一转 `code=500` 并带百度错误信息
- [x] 密钥只写在 `application-local.yml`（已 gitignore），仓库只留模板

真实验证：用 GDI+ 生成一张写着「阿司匹林肠溶片 / 华法林钠片」的图片上传，OCR 正确识别 2 行文字，并归一为「阿司匹林（抗血小板）」「华法林（抗凝血）」。

## 第 4 天（10 月 5 日）交付与验收：药物相互作用提醒（核心创新点）

乔（后端）当日任务：把相互作用检查规则引擎接入后端，写「检查冲突」接口。

- [x] 新增规则表 `interaction_rule`，内置 **11 条真实规则**（含严重程度、机制说明、用药建议、说明书依据），随服务启动自动初始化、可重复执行
- [x] `POST /api/interactions/check`：按老人检查，自动读取其全部进行中用药计划
- [x] `POST /api/interactions/check-drugs`：按药名检查，用于开药前预检、前端加药即时提示
- [x] `GET /api/interactions/rules`：规则库列表，答辩演示用
- [x] 严重程度三级：`1 严重 / 2 警告 / 3 注意`，结果按严重程度排序并给出 `maxSeverity` 供前端标红/弹窗
- [x] 除相互作用外，额外实现**重复用药检测**（同一标准药名出现多次）
- [x] 停用的用药计划不参与冲突检查

真实验证：华法林 + 阿司匹林 → 判定「严重：出血风险显著增加」，附机制、建议与《华法林钠片说明书》依据；阿司匹林出现两次 → 追加「重复用药」警告。

## 第 5~6 天（10 月 6-7 日）：联调与收尾

- [x] 跨域放行本机任意端口（`allowedOriginPatterns: http://localhost:[*]`），赵换端口不用改后端
- [x] 接口文档补全 OCR 与相互作用章节，前端照着写即可
- [x] 补齐 `docs/design.md`（系统设计说明书）与 `docs/test-report.md`（测试报告）
- [x] 冒烟脚本扩到 24 组用例、68 项断言，覆盖全部接口（含真实 OCR 调用）
- [x] 每天提交 Git，远端仓库 `https://gitee.com/nianqingdeqiaojiangjun/aiuse`

> 前后端联调时如遇问题，优先看 `docs/api.md` 的请求/返回示例，以及 `docs/test-report.md` 的缺陷记录。

## 前端（原分工是赵的部分，本次一并完成）

工程位于 `frontend/`，Vue 3 + Vite 7 + Element Plus，共六个页面：

| 页面 | 路由 | 对应文档要求 |
| --- | --- | --- |
| 首页 | `#/home` | 选择当前老人、四个功能入口大按钮 |
| 老人档案 | `#/elders` | 添加老人 / 修改 / 搜索 / 删除（第二天任务） |
| 用药计划 | `#/plans` | 加药、改药、停用恢复、删除（第二天任务） |
| 今日打卡 | `#/checkin` | 今日清单、一键打卡、撤销、按时率、**语音播报**（第二天任务） |
| 拍照识药 | `#/ocr` | 上传/拍照、显示识别结果、一键加入用药计划（第三天任务） |
| 用药安全 | `#/safety` | 冲突检查、**严重项红色标红 + 弹窗**、规则库展示（第四天任务） |

适老化设计（对应郑玲钰的视觉任务，先按高对比方案落地）：

- 正文 18px、标题 28px、行距 1.8，按钮最小高度 56px，所有输入控件加高
- 主色深蓝 `#1F4E79` + 强调明黄 `#FFC000`，白底黑字，警示红 `#D32F2F`，对比度高
- 每个功能都配大按钮与语音播报，减少老人输入成本
- 颜色与字号全部集中在 `frontend/src/styles/theme.css` 的 CSS 变量里，郑玲钰出稿后改这一处即可换肤

技术要点：

- `src/api/index.js` 统一封装 axios：自动解包后端 `{code,message,data}`，出错统一弹中文提示，页面不用重复写错误处理
- 路由用 **hash 模式**，构建版由后端静态托管，刷新页面不会 404
- 开发态 Vite 把 `/api` 代理到 `localhost:8081`，前端零跨域配置；构建态同源，`baseURL` 仍是 `/api`，一份代码两种模式
- 依赖安装走 npmmirror 国内镜像（`frontend/.npmrc`），命令统一用 `npm.cmd`（本机 PowerShell 禁止运行 `npm.ps1`）

验收结果（2026-10-04）：

- `npm.cmd install` 成功（85 个包）
- `npm.cmd run build` 成功且无警告，产物输出到后端 `static/`
- `npm.cmd run dev` 启动正常（Vite 7.3.6），`http://localhost:5173/api/health` 代理连通后端
- 后端启动后 `http://localhost:8081/` 直接返回前端页面，页面引用的 JS/CSS 资源全部 200
- 后端回归：25 个 JUnit 用例 + 68 项冒烟断言全部通过

## 测试（src/test/java）

项目现在有两层测试，分工不同：

| 测试 | 位置 | 特点 |
| --- | --- | --- |
| JUnit 测试 32 个 | `backend/src/test/java/com/aiuse/medreminder/` | 随 `mvn test` 运行，**不需要先启动服务** |
| 接口冒烟测试 70 项断言 | `scripts/smoke-test.ps1` | 打真实 HTTP 接口，端到端验证，需要先启动后端（脚本会自动注册测试账号） |

JUnit 测试按三层组织：

- `MedReminderApplicationTests`：Spring 容器能否启动（1 个）
- `ElderMapperTest`：持久层，验证 SQL 与字段映射、主键回填、逻辑删除（3 个）
- `CheckinServiceTest`：业务层，验证打卡、重复打卡拦截、今日清单展开、按时率、撤销、越权打卡（5 个）
- `ElderApiTest`：接口层（MockMvc），验证统一响应体、参数校验 400、资源不存在 404、健康检查（6 个）
- `DrugDictionaryTest`：药品别名词典，验证商品名归一到标准药名（4 个）
- `InteractionServiceTest`：用药安全，验证严重冲突判定、重复用药、停用计划不参检、规则库完整性（6 个）
- `AuthApiTest`：账号，验证未登录 401、注册即登录、重复用户名、密码错误、退出后失效（5 个）
- `DataIsolationTest`：**数据隔离**，验证 A 账号看不到/改不了/删不了 B 账号的老人、计划与打卡（7 个）

运行方式（项目根目录下）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-backend.ps1
```

测试类都加了 `@Transactional`，**跑完自动回滚，不会往数据库里留下脏数据**；但它们会真实连接本机 MySQL，所以测试前要保证 MySQL 在运行、`application-local.yml` 已配好密码。

## 每日站会（21:00）三句话模板

```
今天做了啥：前端六个页面全部完成（首页 / 老人档案 / 用药计划 / 今日打卡 / 拍照识药 / 用药安全），开发态 npm run dev 与构建版后端托管都跑通，25 个 JUnit 用例 + 68 项冒烟断言全绿。
卡在哪：无。前后端已打通，演示只需双击「启动系统.bat」。
明天做啥：按演示脚本完整走一遍「建老人 → 拍照识药 → 加药 → 冲突标红弹窗 → 打卡 → 语音播报」，录屏并归档材料。
```

## 已知风险

- 本机未安装 Node.js，前端 `npm run dev` 需要赵先安装 Node 18+（建议今天完成）。
- 相互作用规则库只做 8~10 条，演示 1~2 组真实冲突即可。
- 代码每天提交 Git；远端有两个：`origin` = Gitee（团队看代码），`github` = GitHub（Render 部署用）。
- Render 免费实例 15 分钟无人访问会休眠，答辩/演示前先打开一次唤醒。
