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

CREATE TABLE IF NOT EXISTS app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    display_name  VARCHAR(50)  NULL COMMENT '显示名称,如 家属姓名',
    password_hash VARCHAR(200) NOT NULL COMMENT '密码哈希(PBKDF2,格式 iterations:salt:hash)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户';

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
    expire_date   DATE         NULL COMMENT '药品有效期',
    stock_quantity INT         NULL COMMENT '剩余量',
    stock_unit    VARCHAR(10)  NULL COMMENT '剩余量单位,如 片/粒/ml',
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

CREATE TABLE IF NOT EXISTS interaction_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    drug_a      VARCHAR(50)  NOT NULL COMMENT '药品A(标准名)',
    drug_b      VARCHAR(50)  NOT NULL COMMENT '药品B(标准名)',
    severity    TINYINT      NOT NULL COMMENT '严重程度:1严重,2警告,3注意',
    title       VARCHAR(200) NOT NULL COMMENT '冲突标题',
    description VARCHAR(500) NULL COMMENT '机制说明',
    advice      VARCHAR(500) NULL COMMENT '用药建议',
    source      VARCHAR(200) NULL COMMENT '依据来源',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用:1启用,0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_pair (drug_a, drug_b),
    KEY idx_rule_drug_b (drug_b)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '药物相互作用规则库';

INSERT IGNORE INTO interaction_rule (drug_a, drug_b, severity, title, description, advice, source) VALUES
('华法林', '阿司匹林', 1, '出血风险显著增加',
 '两药均影响凝血功能，联用会显著增加消化道出血与颅内出血风险。',
 '避免自行联用；若医生已开具，需定期监测 INR 并观察牙龈出血、黑便等出血征象。',
 '《华法林钠片说明书》药物相互作用项'),
('华法林', '布洛芬', 1, '出血风险增加',
 '布洛芬抑制血小板聚集并损伤胃黏膜，与华法林联用时出血风险明显升高。',
 '避免联用；需要镇痛时请在医生指导下改用对乙酰氨基酚。',
 '《华法林钠片说明书》药物相互作用项'),
('阿司匹林', '布洛芬', 2, '布洛芬可能削弱阿司匹林的抗血小板作用',
 '布洛芬竞争性占据血小板环氧合酶结合位点，可能降低阿司匹林的心脑血管保护作用，同时增加胃出血风险。',
 '避免长期联用；如需同服，阿司匹林应在布洛芬前至少 2 小时服用。',
 '《阿司匹林肠溶片说明书》药物相互作用项'),
('氯吡格雷', '奥美拉唑', 2, '奥美拉唑可能降低氯吡格雷疗效',
 '奥美拉唑抑制 CYP2C19 酶，影响氯吡格雷体内活化，可能增加心血管不良事件风险。',
 '建议改用泮托拉唑等对 CYP2C19 影响较小的抑酸药。',
 '《硫酸氢氯吡格雷片说明书》药物相互作用项'),
('阿托伐他汀', '克拉霉素', 1, '肌病与横纹肌溶解风险升高',
 '克拉霉素抑制 CYP3A4 酶，使阿托伐他汀血药浓度升高，可能引起肌病甚至横纹肌溶解。',
 '避免联用；如必须联用应监测肌酸激酶，出现肌痛、乏力、酱油色尿立即就医。',
 '《阿托伐他汀钙片说明书》药物相互作用项'),
('辛伐他汀', '胺碘酮', 2, '肌病风险增加',
 '胺碘酮抑制辛伐他汀代谢，联合使用可增加肌病风险。',
 '限制辛伐他汀剂量，并在联用期间监测肌酸激酶。',
 '《辛伐他汀片说明书》药物相互作用项'),
('二甲双胍', '格列本脲', 2, '低血糖风险增加',
 '两种降糖药作用叠加，老年患者更容易发生低血糖。',
 '加强血糖监测，随身携带糖果；出现心慌、出汗、手抖时立即测血糖。',
 '《格列本脲片说明书》药物相互作用项'),
('硝苯地平', '美托洛尔', 2, '低血压与心动过缓风险',
 '两药降压、减慢心率的作用叠加，可能引起头晕、乏力甚至跌倒。',
 '监测血压与心率，起身时动作放缓，避免跌倒。',
 '《硝苯地平控释片说明书》药物相互作用项'),
('地高辛', '呋塞米', 2, '低钾血症诱发地高辛中毒',
 '呋塞米促进钾排出，低钾血症时心肌对地高辛敏感性增加，易出现心律失常。',
 '定期监测血钾与地高辛血药浓度，必要时在医生指导下补钾。',
 '《地高辛片说明书》药物相互作用项'),
('螺内酯', '氯化钾', 3, '高钾血症风险',
 '螺内酯为保钾利尿药，与补钾制剂联用容易导致血钾升高。',
 '监测血钾，避免自行服用补钾制剂。',
 '《螺内酯片说明书》药物相互作用项'),
('阿司匹林', '氯吡格雷', 3, '双联抗血小板治疗需遵医嘱',
 '两药联用是常见的双抗方案，但会明显增加出血风险，需要医生评估获益与风险。',
 '严格按医嘱服用，注意观察皮肤瘀斑、牙龈出血、黑便等出血征象。',
 '《硫酸氢氯吡格雷片说明书》药物相互作用项');
