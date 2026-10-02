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

- 后端：Java 17 + Spring Boot 4.0.0 + JdbcTemplate + MySQL 9.6
- 前端（赵负责）：Vue 3 + Vite，默认 `http://localhost:5173`
- 后端端口：`8080`，接口前缀 `/api`

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
│       │   ├── repository/  # JdbcTemplate 数据访问
│       │   ├── entity/      # 数据库实体
│       │   └── dto/         # 请求参数对象
│       └── resources/
│           ├── application.yml
│           ├── application-local.yml.example
│           └── schema.sql   # 三张核心表
├── docs/api.md              # 接口清单（给赵、陈）
└── scripts/                 # 环境、建库、启动、冒烟脚本
```

## 快速开始

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

启动成功后访问健康检查：<http://localhost:8080/api/health>

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
Invoke-RestMethod http://localhost:8080/api/health
# 2) 全链路冒烟
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

### 第 1 天验收记录（乔，2026-10-02）

- 后端启动成功：Spring Boot 4.0.0 / Spring 7.0.1 / Tomcat 11 / 端口 8080
- `GET /api/health` 返回 `status=UP, db=UP, mysqlVersion=9.6.0, schema=med_reminder`
- `scripts/smoke-test.ps1` 全部 10 组用例、19 项断言通过（含中文姓名「张三」写入/回读、重复打卡拦截、缺参 400、未知 id 404、逻辑删除）
- 持久化验证：重启服务后，老人/用药计划/打卡记录仍可查询
- 数据库现有 1 条演示数据（老人「张三」+ 阿司匹林计划 + 今日打卡），方便赵做前端联调

清空演示数据（可选，按外键顺序删除）：

```sql
DELETE FROM medication_log; DELETE FROM medication_plan; DELETE FROM elder;
```

## 每日站会（21:00）三句话模板

```
今天做了啥：后端骨架跑通，health 接口 db=UP，老人/用药/打卡三段接口冒烟通过，接口文档已发群。
卡在哪：无（风险提示：本机未装 Node.js，赵需要今天装好，否则明天前端联调会卡）。
明天做啥：完成老人档案 + 用药计划 + 打卡后端接口的完整版，配合陈做接口测试。
```

## 已知风险

- 本机未安装 Node.js，前端 `npm run dev` 需要赵先安装 Node 18+（建议今天完成）。
- 相互作用规则库只做 8~10 条，演示 1~2 组真实冲突即可。
- 代码每天提交 Git；Gitee 远端仓库需团队创建后再配置 `git remote`。
