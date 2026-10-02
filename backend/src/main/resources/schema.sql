CREATE TABLE IF NOT EXISTS elder (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(50)  NOT NULL COMMENT '老人姓名',
    age             INT          NULL COMMENT '年龄',
    gender          VARCHAR(10)  NULL COMMENT '性别',
    phone           VARCHAR(20)  NULL COMMENT '联系电话',
    chronic_disease VARCHAR(255) NULL COMMENT '基础疾病',
    note            VARCHAR(500) NULL COMMENT '备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常,1已删除',
    PRIMARY KEY (id),
    KEY idx_elder_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '老人档案';

CREATE TABLE IF NOT EXISTS medication_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    elder_id      BIGINT       NOT NULL COMMENT '老人ID',
    drug_name     VARCHAR(100) NOT NULL COMMENT '药品名称',
    dosage        VARCHAR(50)  NOT NULL COMMENT '剂量,如 1片',
    frequency     VARCHAR(50)  NULL COMMENT '服用频次,如 每日2次',
    take_relation VARCHAR(20)  NULL COMMENT '服用时机:饭前/饭后/睡前',
    remind_times  VARCHAR(100) NULL COMMENT '提醒时间,如 08:00,20:00',
    start_date    DATE         NULL COMMENT '开始日期',
    end_date      DATE         NULL COMMENT '结束日期',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1进行中,0已停用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常,1已删除',
    PRIMARY KEY (id),
    KEY idx_plan_elder (elder_id),
    CONSTRAINT fk_plan_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用药计划';

CREATE TABLE IF NOT EXISTS medication_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    elder_id    BIGINT      NOT NULL COMMENT '老人ID',
    plan_id     BIGINT      NOT NULL COMMENT '用药计划ID',
    take_date   DATE        NOT NULL COMMENT '打卡日期',
    remind_time VARCHAR(10) NOT NULL DEFAULT '08:00' COMMENT '该次提醒时间,如 08:00',
    take_time   DATETIME    NULL COMMENT '实际服药时间',
    status      VARCHAR(20) NOT NULL DEFAULT 'TAKEN' COMMENT '状态:TAKEN已服/MISSED未服',
    note        VARCHAR(200) NULL COMMENT '备注',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_plan_date_time (plan_id, take_date, remind_time),
    KEY idx_log_elder_date (elder_id, take_date),
    CONSTRAINT fk_log_elder FOREIGN KEY (elder_id) REFERENCES elder (id),
    CONSTRAINT fk_log_plan FOREIGN KEY (plan_id) REFERENCES medication_plan (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '服药打卡记录';

