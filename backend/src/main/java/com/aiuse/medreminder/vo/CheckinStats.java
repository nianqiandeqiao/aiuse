package com.aiuse.medreminder.vo;

import java.time.LocalDate;

/**
 * 服药打卡统计：应服次数、已服次数、漏服次数与按时率。
 */
public record CheckinStats(
        Long elderId,
        LocalDate from,
        LocalDate to,
        int expected,
        int taken,
        int missed,
        double rate) {
}

