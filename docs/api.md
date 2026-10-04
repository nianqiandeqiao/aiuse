# 后端接口清单（第 2 天完成版）

服务地址：`http://localhost:8081`，所有接口前缀 `/api`。

## 统一响应体

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

- `code`：`0` 成功，`400` 参数/业务错误，`404` 资源不存在，`500` 服务内部错误
- HTTP 状态码统一为 `200`，前端**只需判断 `code`**
- 日期格式统一 `yyyy-MM-dd`；时间 `HH:mm`；日期时间 `yyyy-MM-ddTHH:mm:ss`

## 1. 健康检查

`GET /api/health`

```json
{
  "code": 0, "message": "ok",
  "data": { "status": "UP", "db": "UP", "mysqlVersion": "9.6.0", "schema": "med_reminder" }
}
```

## 2. 老人档案

### 2.1 新增老人 `POST /api/elders`

```json
{
  "name": "张三",
  "age": 78,
  "gender": "M",
  "phone": "13800001234",
  "chronicDisease": "高血压",
  "note": "独居"
}
```

必填 `name`；`age` 范围 0~130，超过返回 `code=400`。

### 2.2 老人列表 / 搜索 `GET /api/elders?keyword=张`

`keyword` 可选，按姓名、电话、基础疾病模糊匹配（三个字段任意命中即可）；不传则返回全部。按 `id` 倒序，只含未删除记录，无数据时 `data` 为 `[]`。

### 2.3 老人详情 `GET /api/elders/{id}`

不存在返回 `{"code":404,"message":"老人档案不存在: id=1","data":null}`。

### 2.4 修改老人 `PUT /api/elders/{id}`

请求体字段与新增完全一致，返回修改后的完整对象。

### 2.5 删除老人 `DELETE /api/elders/{id}`

逻辑删除。同时会**级联停用该老人的全部用药计划**，避免打卡页出现孤儿计划；重复删除返回 `code=404`。

## 3. 用药计划

### 3.1 新增用药计划 `POST /api/plans`

```json
{
  "elderId": 1,
  "drugName": "阿司匹林",
  "dosage": "1片",
  "frequency": "每日2次",
  "takeRelation": "饭后",
  "remindTimes": "08:00,20:00",
  "startDate": "2026-10-03",
  "endDate": "2026-10-31"
}
```

- 必填：`elderId`、`drugName`、`dosage`
- `remindTimes` 格式必须是 `HH:mm` 或 `HH:mm,HH:mm`（如 `08:00,20:00`），格式错误返回 `code=400`；不填默认 `08:00`
- `startDate` / `endDate` 可选，用于限定计划生效区间
- `elderId` 不存在返回 `code=404`

### 3.2 按老人查询计划 `GET /api/plans?elderId=1`

返回该老人全部未删除计划（含已停用的），按 `id` 倒序。计划对象里 `status`：`1` 进行中，`0` 已停用。

### 3.3 计划详情 `GET /api/plans/{id}`

### 3.4 修改计划 `PUT /api/plans/{id}`

请求体与新增一致，但**不包含 `elderId`**（计划归属不允许修改）。

### 3.5 停用 / 恢复计划 `PUT /api/plans/{id}/status?status=0`

`status=0` 停用，`status=1` 恢复；其他值返回 `code=400`。停用后该计划不再出现在今日清单里。

### 3.6 删除计划 `DELETE /api/plans/{id}`

逻辑删除，重复删除返回 `code=404`。

### 3.7 今日服药清单 `GET /api/plans/today?elderId=1&date=2026-10-03`

**打卡页的核心接口**。`date` 不传默认今天。把该老人所有"进行中且在有效期内"的计划，按 `remindTimes` 展开成一个个时间点，并标注是否已打卡：

```json
{
  "code": 0, "message": "ok",
  "data": {
    "elderId": 1,
    "elderName": "张三",
    "date": "2026-10-03",
    "total": 2,
    "taken": 1,
    "pending": 1,
    "items": [
      { "planId": 1, "drugName": "阿司匹林", "dosage": "1片", "frequency": "每日2次",
        "takeRelation": "饭后", "remindTime": "08:00", "taken": true,
        "logId": 5, "status": "TAKEN", "takeTime": "2026-10-03T08:03:11" },
      { "planId": 1, "drugName": "阿司匹林", "dosage": "1片", "frequency": "每日2次",
        "takeRelation": "饭后", "remindTime": "20:00", "taken": false,
        "logId": null, "status": null, "takeTime": null }
    ]
  }
}
```

`items` 按提醒时间升序排列；页面顶部的"今日 1/2 已完成"直接用 `taken` / `total` 即可。

## 4. 服药打卡

### 4.1 打卡 `POST /api/checkins`

```json
{
  "elderId": 1,
  "planId": 1,
  "takeDate": "2026-10-03",
  "remindTime": "08:00",
  "status": "TAKEN",
  "note": ""
}
```

- `takeDate` 必填；`remindTime` 默认 `08:00`（格式 `HH:mm`）；`status` 默认 `TAKEN`，可选 `TAKEN` / `MISSED`
- 同一计划 + 同一天 + 同一提醒时间重复打卡返回 `code=400`
- `planId` 不属于该老人返回 `code=400`

### 4.2 查询打卡记录 `GET /api/checkins?elderId=1&date=2026-10-03`

三种用法，优先级从高到低：

- 传 `date`：查这一天
- 传 `from` + `to`：查区间，如 `?elderId=1&from=2026-10-01&to=2026-10-07`
- 都不传：返回该老人全部打卡记录

### 4.3 按时率统计 `GET /api/checkins/stats?elderId=1&from=2026-10-01&to=2026-10-07`

`from` / `to` 都不传时默认统计最近 7 天；区间上限 92 天，超过返回 `code=400`。

```json
{
  "code": 0, "message": "ok",
  "data": { "elderId": 1, "from": "2026-10-03", "to": "2026-10-03",
            "expected": 2, "taken": 1, "missed": 1, "rate": 0.5 }
}
```

- `expected`：应服次数（进行中计划 × 区间内每天的提醒点数）
- `taken`：已服次数（区间内状态为 `TAKEN` 的记录数）
- `missed`：漏服次数（`expected - taken`，不为负）
- `rate`：按时率，保留 3 位小数，如 `0.5` 表示 50%

### 4.4 撤销打卡 `DELETE /api/checkins/{id}`

用于误打卡后回退，物理删除该条记录；记录不存在返回 `code=404`。

## 5. 拍照识药（OCR）

### 5.1 上传图片识别药名 `POST /api/ocr/drug`

`multipart/form-data`，字段名 `file`，单张图片不超过 4MB（百度接口限制）。

```json
{
  "code": 0, "message": "ok",
  "data": {
    "rawText": "阿司匹林肠溶片\n华法林钠片",
    "lines": ["阿司匹林肠溶片", "华法林钠片"],
    "drugs": [
      { "standardName": "阿司匹林", "category": "抗血小板", "matchedKeyword": "阿司匹林" },
      { "standardName": "华法林", "category": "抗凝血", "matchedKeyword": "华法林" }
    ],
    "lineCount": 2,
    "drugCount": 2
  }
}
```

处理链路：`图片 → 百度通用文字识别(general_basic) → 文本行 → 药品别名词典归一 → 标准药名`。

- `drugs[].standardName` 可以直接拿去调 `POST /api/plans` 的 `drugName` 字段
- 词典文件：`backend/src/main/resources/drug-dictionary.txt`（35 种常用药、97 个关键词），新增药品只需加一行，不用改代码
- 图片过大返回 `code=400`；未配置密钥或百度接口报错返回 `code=500` 并附带原因

### 5.2 base64 方式识别 `POST /api/ocr/base64`

```json
{ "imageBase64": "data:image/png;base64,iVBORw0KGgo..." }
```

带不带 `data:image/...;base64,` 前缀都可以，后端会自动处理。适合小程序或相机插件拿不到文件流的场景。

## 6. 药物相互作用检查（核心创新点）

### 6.1 按老人检查 `POST /api/interactions/check`

```json
{ "elderId": 1 }
```

后端自动读取该老人**所有进行中的用药计划**，两两组合匹配规则库，并额外检测重复用药。

### 6.2 按药名检查 `POST /api/interactions/check-drugs`

```json
{ "drugNames": ["华法林", "阿司匹林"] }
```

用于开药前预检、前端"添加药品"时即时提示。药名可以是商品名或带剂型的写法，后端会自动归一。

### 6.3 返回结构（上面两个接口相同）

```json
{
  "code": 0, "message": "ok",
  "data": {
    "elderId": null,
    "drugs": ["华法林", "阿司匹林"],
    "drugCount": 2,
    "conflictCount": 1,
    "maxSeverity": 1,
    "maxSeverityLabel": "严重",
    "conflicts": [
      {
        "type": "INTERACTION",
        "drugA": "华法林",
        "drugB": "阿司匹林",
        "severity": 1,
        "severityLabel": "严重",
        "title": "出血风险显著增加",
        "description": "两药均影响凝血功能，联用会显著增加消化道出血与颅内出血风险。",
        "advice": "避免自行联用；若医生已开具，需定期监测 INR 并观察牙龈出血、黑便等出血征象。",
        "source": "《华法林钠片说明书》药物相互作用项"
      }
    ]
  }
}
```

- `severity`：`1` 严重、`2` 警告、`3` 注意（数字越小越严重，`conflicts` 已按严重程度升序排好）
- `type`：`INTERACTION` 相互作用 / `DUPLICATE` 重复用药
- 前端建议：`maxSeverity=1` 红色弹窗 + 语音播报，`2` 黄色提醒，`3` 灰色提示；无冲突时 `conflictCount=0`、`maxSeverity=null`

### 6.4 规则库 `GET /api/interactions/rules`

返回全部规则（含已停用的），字段：`drugA`、`drugB`、`severity`、`title`、`description`、`advice`、`source`、`enabled`。当前内置 **11 条真实规则**，覆盖华法林、阿司匹林、氯吡格雷、他汀类、降糖药、降压药、地高辛等常见组合。

> 免责声明：规则库依据药品说明书整理，仅用于课程演示与用药提醒，不能替代医生或药师的处方审核。

## 7. 数据表

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `elder` | 老人档案 | name, age, gender, phone, chronic_disease, deleted |
| `medication_plan` | 用药计划 | elder_id, drug_name, dosage, frequency, take_relation, remind_times, status, deleted |
| `medication_log` | 服药打卡 | elder_id, plan_id, take_date, remind_time, status（唯一索引 plan_id+take_date+remind_time） |
| `interaction_rule` | 相互作用规则库 | drug_a, drug_b, severity, title, description, advice, source, enabled（唯一索引 drug_a+drug_b） |

建表与规则初始化脚本：`backend/src/main/resources/schema.sql`，服务启动时自动执行（全部 `CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE`，可重复执行）。

## 8. 自测脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

覆盖全部接口共 **24 组用例、68 项断言**：中文姓名写入回读、老人档案增删改查、用药计划增改停用、今日服药清单展开、打卡与重复打卡拦截、按时率统计、误打卡撤销、参数校验 400、未知资源 404、**真实调用百度 OCR 识别药名**、**华法林+阿司匹林严重冲突判定**、规则库条数校验等。

## 9. 后续可扩展方向

- 百度语音合成后端代理（前端若不用浏览器 `speechSynthesis` 时可切换）
- 相互作用规则库接入权威数据源（如国家药品监督管理局数据）、补充食物-药物相互作用
- 打卡提醒推送（微信服务号/短信）、家属端账号与用药周报
- 与社区医院 HIS/电子处方系统对接，实现处方自动导入与审核
