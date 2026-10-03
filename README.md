# AI 用药提醒与用药安全助手

国庆 6 天冲刺项目（10 月 2 日 — 10 月 7 日），目标：做出一版能现场演示的雏形。

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
- 前端（赵负责）：Vue 3 + Vite，默认 `http://localhost:5173`
- 后端端口：`8081`，接口前缀 `/api`

## 目录结构

```
aiuse/
├── backend/                 # 后端 Maven 工程
│   └── src/main/
│       ├── java/com/aiuse/medreminder/
│       │   ├── common/      # 统一响应体、全局异常
│       │   ├── config/      # CORS、建表初始化
│       │   ├── controller/  # REST 接口
│       │   ├── service/     # 业务逻辑
│       │   ├── mapper/      # MyBatis Mapper 接口（持久层）
│       │   ├── entity/      # 数据库实体
│       │   ├── dto/         # 请求参数对象
│       │   └── vo/          # 返回给前端的结果对象（今日清单、统计）
│       └── resources/
│           ├── application.yml
│           ├── application-local.yml.example
│           ├── schema.sql   # 三张核心表
│           └── mapper/      # MyBatis SQL 映射文件（*.xml）
├── docs/api.md              # 接口清单（给赵、陈）
└── scripts/                 # 环境、建库、启动、冒烟脚本
```

## 快速开始

> 小白版：直接双击项目根目录的 `启动后端.bat`（黑窗口别关，关掉就等于停服务），
> 然后双击 `冒烟测试.bat` 看是否全绿。下面是用命令行的完整说明。

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
- [x] `scripts/smoke-test.ps1` 扩到 20 组用例、53 项断言，全部通过

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

## 每日站会（21:00）三句话模板

```
今天做了啥：老人档案/用药计划/打卡三块后端接口全部完成（含修改、停用、撤销打卡、按时率统计、今日服药清单），并按团队要求把持久层从 JdbcTemplate 换成 MyBatis，冒烟测试 53 项断言全绿，接口文档已更新发群。
卡在哪：无（待跟进赵的前端页面进度、陈的百度云 OCR 审核是否通过）。
明天做啥：接百度 OCR，做「上传照片 → 识别药名」接口，并配合陈准备真实药盒测试素材。
```

## 已知风险

- 本机未安装 Node.js，前端 `npm run dev` 需要赵先安装 Node 18+（建议今天完成）。
- 相互作用规则库只做 8~10 条，演示 1~2 组真实冲突即可。
- 代码每天提交 Git；Gitee 远端仓库需团队创建后再配置 `git remote`。
