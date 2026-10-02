# 后端接口清单（第 1 天）

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
- 未登录/鉴权：本期雏形不做，后续如需要再补

## 1. 健康检查

`GET /api/health`

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "status": "UP",
    "time": "2026-10-02T20:10:00",
    "db": "UP",
    "mysqlVersion": "9.6.0",
    "schema": "med_reminder"
  }
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

必填：`name`。返回 `data` 为创建后的完整老人对象（含 `id`、`createdAt`）。

### 2.2 老人列表 `GET /api/elders`

返回 `data` 为老人数组，按 `id` 倒序，只含未删除记录；无数据时 `data` 为 `[]`。

### 2.3 老人详情 `GET /api/elders/{id}`

不存在返回 `{"code":404,"message":"老人档案不存在: id=1","data":null}`。

### 2.4 删除老人 `DELETE /api/elders/{id}`

逻辑删除（`deleted=1`），重复删除返回 `code=404`。

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
  "startDate": "2026-10-02",
  "endDate": "2026-10-31"
}
```

必填：`elderId`、`drugName`、`dosage`。`elderId` 不存在返回 `code=404`。

### 3.2 按老人查询计划 `GET /api/plans?elderId=1`

`data` 为计划数组，按 `id` 倒序。

### 3.3 计划详情 `GET /api/plans/{id}`

## 4. 服药打卡

### 4.1 打卡 `POST /api/checkins`

```json
{
  "elderId": 1,
  "planId": 1,
  "takeDate": "2026-10-02",
  "remindTime": "08:00",
  "status": "TAKEN",
  "note": ""
}
```

- `takeDate` 必填，格式 `yyyy-MM-dd`
- `remindTime` 可选，默认 `08:00`，格式 `HH:mm`
- `status` 可选，默认 `TAKEN`，可选值 `TAKEN` / `MISSED`
- 同一计划 + 同一天 + 同一提醒时间重复打卡返回 `code=400`

### 4.2 查询打卡记录 `GET /api/checkins?elderId=1&date=2026-10-02`

`date` 可选；不传则返回该老人全部打卡记录。

## 5. 数据表

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `elder` | 老人档案 | name, age, gender, phone, chronic_disease, deleted |
| `medication_plan` | 用药计划 | elder_id, drug_name, dosage, frequency, take_relation, remind_times |
| `medication_log` | 服药打卡 | elder_id, plan_id, take_date, remind_time, status |

建表脚本：`backend/src/main/resources/schema.sql`，服务启动时自动执行（`CREATE TABLE IF NOT EXISTS`）。

## 6. 今日未做（按规划后续实现）

- 拍照识药 OCR（第 3 天）
- 语音播报（第 2~3 天，前端主导）
- 药物相互作用检查（第 4 天，核心创新点）
- 登录鉴权、分页、批量导入

