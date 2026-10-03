package com.aiuse.medreminder.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药计划实体，字段与 medication_plan 表一一对应。
 */
@Data
public class MedicationPlan {

    private Long id;
    private Long elderId;
    private String drugName;
    private String dosage;
    private String frequency;
    private String takeRelation;
    private String remindTimes;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 1 进行中，0 已停用。 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}

