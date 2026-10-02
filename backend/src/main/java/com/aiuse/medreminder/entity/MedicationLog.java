package com.aiuse.medreminder.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服药打卡记录。
 */
public record MedicationLog(
        Long id,
        Long elderId,
        Long planId,
        LocalDate takeDate,
        String remindTime,
        LocalDateTime takeTime,
        String status,
        String note,
        LocalDateTime createdAt) {
}

