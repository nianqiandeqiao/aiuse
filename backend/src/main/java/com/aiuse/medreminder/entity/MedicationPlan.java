package com.aiuse.medreminder.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药计划。
 */
public record MedicationPlan(
        Long id,
        Long elderId,
        String drugName,
        String dosage,
        String frequency,
        String takeRelation,
        String remindTimes,
        LocalDate startDate,
        LocalDate endDate,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean deleted) {
}

