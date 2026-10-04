# AI 用药提醒与用药安全助手

国庆 6 天冲刺项目（10 月 2 日 — 10 月 7 日），目标：做出一版能现场演示的雏形。

## 项目亮点

1. **拍照识药**：拍药盒照片自动识别药名，并用药品别名词典把"拜阿司匹灵""阿司匹林肠溶片"统一归一到"阿司匹林"
2. **用药安全提醒（核心创新点）**：自动检查老人当前全部用药的相互作用与重复用药，每条冲突都给出严重程度、机制说明、可执行建议和说明书依据
3. **适老化闭环**：拍照识药（不用打字）→ 今日服药清单（按时段排好）→ 一键打卡 → 语音播报
4. 分层规范、测试完备：`controller → service → mapper → entity`，25 个 JUnit 测试 + 68 项端到端断言

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
| JUnit 测试 25 个 | `backend/src/test/java/com/aiuse/medreminder/` | 随 `mvn test` 运行，**不需要先启动服务** |
| 接口冒烟测试 68 项断言 | `scripts/smoke-test.ps1` | 打真实 HTTP 接口，端到端验证，需要先启动后端 |

JUnit 测试按三层组织：

- `MedReminderApplicationTests`：Spring 容器能否启动（1 个）
- `ElderMapperTest`：持久层，验证 SQL 与字段映射、主键回填、逻辑删除（3 个）
- `CheckinServiceTest`：业务层，验证打卡、重复打卡拦截、今日清单展开、按时率、撤销、越权打卡（5 个）
- `ElderApiTest`：接口层（MockMvc），验证统一响应体、参数校验 400、资源不存在 404、健康检查（6 个）
- `DrugDictionaryTest`：药品别名词典，验证商品名归一到标准药名（4 个）
- `InteractionServiceTest`：用药安全，验证严重冲突判定、重复用药、停用计划不参检、规则库完整性（6 个）

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
- 代码每天提交 Git；Gitee 远端仓库需团队创建后再配置 `git remote`。
