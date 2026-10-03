# 后端接口清单（第 2 天完成版）

服务地址：`http://localhost:8080`，所有接口前缀 `/api`。

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

## 5. 数据表

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `elder` | 老人档案 | name, age, gender, phone, chronic_disease, deleted |
| `medication_plan` | 用药计划 | elder_id, drug_name, dosage, frequency, take_relation, remind_times, status, deleted |
| `medication_log` | 服药打卡 | elder_id, plan_id, take_date, remind_time, status（唯一索引 plan_id+take_date+remind_time） |

建表脚本：`backend/src/main/resources/schema.sql`，服务启动时自动执行（`CREATE TABLE IF NOT EXISTS`）。

## 6. 自测脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

覆盖上面全部接口共 20 组用例、53 项断言（含中文姓名写入回读、重复打卡拦截、非法提醒时间格式、计划停用后今日清单清空、按时率计算、误打卡撤销、逻辑删除等）。

## 7. 第 3~4 天待做（按规划）

- 拍照识药 OCR（第 3 天，接百度通用文字识别）
- 语音播报（浏览器 `speechSynthesis` 即可，前端负责；如需百度语音合成再补后端代理）
- 药物相互作用检查（第 4 天，核心创新点，Java 规则服务）

