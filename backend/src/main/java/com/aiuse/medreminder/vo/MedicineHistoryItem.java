package com.aiuse.medreminder.vo;

import java.time.LocalDate;

/**
 * 历史用药记录：停药或删除后仍然保留，方便子女/医生回查。
 */
public record MedicineHistoryItem(
        Long planId,
        String drugName,
        String dosage,
        String frequency,
        String takeRelation,
        String remindTimes,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate expireDate,
        Integer stockQuantity,
        String stockUnit,
        Integer status,
        Boolean deleted,
        Integer totalLogs,
        Integer takenLogs) {
}

