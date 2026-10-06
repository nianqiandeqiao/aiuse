package com.aiuse.medreminder.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * 服药依从性报告（周报/月报）。
 * 子女一眼能看到"这周漏服几次"，医生能看到依从性百分比。
 */
public record AdherenceReport(
        String period,
        LocalDate from,
        LocalDate to,
        Long elderId,
        String elderName,
        int expected,
        int taken,
        int missed,
        double rate,
        int missedDays,
        String summary,
        List<DailyAdherence> daily,
        List<DrugAdherence> byDrug) {
}

