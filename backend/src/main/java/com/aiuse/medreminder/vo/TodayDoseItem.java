package com.aiuse.medreminder.vo;

import java.time.LocalDateTime;

/**
 * 今日服药清单中的一条：某个用药计划的一个提醒时间点。
 */
public record TodayDoseItem(
        Long planId,
        String drugName,
        String dosage,
        String frequency,
        String takeRelation,
        String remindTime,
        boolean taken,
        Long logId,
        String status,
        LocalDateTime takeTime) {
}

