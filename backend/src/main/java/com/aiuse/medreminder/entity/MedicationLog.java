package com.aiuse.medreminder.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服药打卡记录实体，字段与 medication_log 表一一对应。
 */
@Data
public class MedicationLog {

    private Long id;
    private Long elderId;
    private Long planId;
    private LocalDate takeDate;
    private String remindTime;
    private LocalDateTime takeTime;
    /** TAKEN 已服 / MISSED 未服。 */
    private String status;
    private String note;
    private LocalDateTime createdAt;
}

