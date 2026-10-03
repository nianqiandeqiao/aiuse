package com.aiuse.medreminder.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * 某位老人某一天的服药清单汇总，前端"打卡页"直接消费这个结构。
 */
public record TodayDoseSummary(
        Long elderId,
        String elderName,
        LocalDate date,
        int total,
        int taken,
        int pending,
        List<TodayDoseItem> items) {
}

